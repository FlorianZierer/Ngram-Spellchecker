package generator;

import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;
import util.PredictionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

// Diese Klasse implementiert Callable und ist für das Laden von N-Grammen verantwortlich
public class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private final Path jsonFilePath;
    private final List<Texture<Script>> ngramsToSearch;
    private final double acceptanceThreshold;
    private final int startNgramIndex;
    private final int endNgramIndex;
    private final int ngramSize;
    private final List<Prediction> mutablePredictions;

    // Konstruktor für die Klasse
    public LoadNgramCallable(Path jsonFilePath, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int startNgramIndex, int endNgramIndex) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = paddedWords.grammy(ngrams);
        this.acceptanceThreshold = acceptanceThreshold;
        this.startNgramIndex = startNgramIndex;
        this.endNgramIndex = endNgramIndex;
        this.ngramSize = ngrams;
        this.mutablePredictions = new ArrayList<>(predictions.toList());
    }

    // Überschriebene call-Methode, die von Callable gefordert wird
    @Override
    public Texture<Prediction> call() throws Exception {
        loadExistingNgrams();
        return new Texture<>(mutablePredictions);
    }

    // Verarbeitet N-Gramme innerhalb des zugewiesenen Bereichs
    private void processNgrams(BufferedReader reader) throws IOException {
        int ngramIndex = 0;
        while (ngramIndex < endNgramIndex) {
            if (skipToNextNgram(reader)) {
                if (ngramIndex >= startNgramIndex) {
                    processNgram(reader);
                } else {
                    skipNgram(reader);
                }
                ngramIndex++;
            } else {
                break;
            }
        }
    }

    // Verarbeitet ein einzelnes N-Gramm
    // Diese Methode ist speichereffizient, da sie das N-Gramm zeichenweise liest und verarbeitet,
    // ohne den gesamten JSON-Inhalt im Speicher zu halten
    private void processNgram(BufferedReader reader) throws IOException {
        StringBuilder ngramBuilder = new StringBuilder("[");
        int nestingLevel = 1;
        boolean inString = false;
        boolean escape = false;

        while (nestingLevel > 0) {
            int c = reader.read();
            if (c == -1) {
                throw new IOException("Unexpected end of file");
            }
            char ch = (char) c;
            ngramBuilder.append(ch);

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
            } else {
                if (ch == '"') {
                    inString = true;
                } else if (ch == '[') {
                    nestingLevel++;
                } else if (ch == ']') {
                    nestingLevel--;
                }
            }
        }

        Nexus.DataNote ngramNote = Nexus.DataNote.byJSON(ngramBuilder.toString());
        Texture<Script> ngram = new Texture<>(ngramNote.asList(inner -> new Script(inner.asString())));
        filterForSuggestions(ngram);
    }

    // Überspringt ein N-Gramm ohne es zu verarbeiten
    // Diese Methode ist ebenfalls speichereffizient, da sie das N-Gramm zeichenweise überspringt,
    // ohne es im Speicher zu halten
    private void skipNgram(BufferedReader reader) throws IOException {
        int nestingLevel = 1;
        boolean inString = false;
        boolean escape = false;

        while (nestingLevel > 0) {
            int c = reader.read();
            if (c == -1) {
                throw new IOException("Unexpected end of file");
            }
            char ch = (char) c;

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
            } else {
                if (ch == '"') {
                    inString = true;
                } else if (ch == '[') {
                    nestingLevel++;
                } else if (ch == ']') {
                    nestingLevel--;
                }
            }
        }
    }

    // Lädt existierende N-Gramme aus der JSON-Datei
    private void loadExistingNgrams() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            skipWhitespaceAndOpenBracket(reader);
            processNgrams(reader);
        }
    }

    // Überspringt Leerzeichen und die öffnende eckige Klammer
    private void skipWhitespaceAndOpenBracket(BufferedReader reader) throws IOException {
        int c;
        while ((c = reader.read()) != -1 && Character.isWhitespace(c)) {
            // Überspringe Leerzeichen
        }
        if (c != '[') {
            throw new IOException("Expected '[' at the beginning of the JSON array");
        }
    }

    // Springt zum nächsten N-Gramm in der JSON-Datei
    private boolean skipToNextNgram(BufferedReader reader) throws IOException {
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '[') {
                return true;
            } else if (c == ']') {
                return false;
            }
        }
        return false;
    }

    // Filtert N-Gramme für Vorschläge
    private void filterForSuggestions(Texture<Script> inputNgram) {
        for (int i = 0; i < ngramsToSearch.size(); i++) {
            getSuggestion(ngramsToSearch.get(i), inputNgram, i);
        }
    }

    // Ermittelt Vorschläge basierend auf den N-Grammen
    private void getSuggestion(Texture<Script> input, Texture<Script> data, int predictionIndex) {
        if (input.extent() != ngramSize || data.extent() != ngramSize) {
            return;
        }

        double[] distances = new double[3];
        boolean[] distanceValid = new boolean[3];

        for (int i = 0; i < 3; i++) {
            distances[i] = PredictionUtils.distance(input.at(i), data.at(i));
            distanceValid[i] = distances[i] >= acceptanceThreshold;
        }

        Prediction prediction = mutablePredictions.get(predictionIndex);
        if (distanceValid[0] && distanceValid[1] && distanceValid[2]) {
            prediction.addSuggestionTriGram(new Suggestion(distances[1], data.at(1)));
        } else if ((distanceValid[0] || distanceValid[2]) && distanceValid[1]) {
            prediction.addSuggestionBiGram(new Suggestion(distances[1], data.at(1)));
        } else if (distanceValid[1]) {
            prediction.addSuggestionDirect(new Suggestion(distances[1], data.at(1)));
        }
    }
}