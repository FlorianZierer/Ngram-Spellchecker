package generator;

import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private final Path jsonFilePath;
    private final List<Texture<Script>> ngramsToSearch;
    private final double acceptanceThreshold;
    private final int startNgramIndex;
    private final int endNgramIndex;
    private final int ngramSize;
    private final List<Prediction> mutablePredictions;

    public LoadNgramCallable(Path jsonFilePath, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int startNgramIndex, int endNgramIndex) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = paddedWords.grammy(ngrams);
        this.acceptanceThreshold = acceptanceThreshold;
        this.startNgramIndex = startNgramIndex;
        this.endNgramIndex = endNgramIndex;
        this.ngramSize = ngrams;
        this.mutablePredictions = new ArrayList<>(predictions.toList());
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        loadExistingNgrams();
        return new Texture<>(mutablePredictions);
    }

    private void loadExistingNgrams() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            skipWhitespaceAndOpenBracket(reader);
            processNgrams(reader);
        }
    }

    private void skipWhitespaceAndOpenBracket(BufferedReader reader) throws IOException {
        int c;
        while ((c = reader.read()) != -1 && Character.isWhitespace(c)) {
            // Skip whitespace
        }
        if (c != '[') {
            throw new IOException("Expected '[' at the beginning of the JSON array");
        }
    }

    private void processNgrams(BufferedReader reader) throws IOException {
        int ngramIndex = 0;
        while (ngramIndex < endNgramIndex) {
            if (skipToNextNgram(reader)) {
                if (ngramIndex >= startNgramIndex) {
                    processNgram(reader);
                }
                ngramIndex++;
            } else {
                break;
            }
        }
    }

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

    private void filterForSuggestions(Texture<Script> inputNgram) {
        for (int i = 0; i < ngramsToSearch.size(); i++) {
            getSuggestion(ngramsToSearch.get(i), inputNgram, i);
        }
    }

    private void getSuggestion(Texture<Script> input, Texture<Script> data, int predictionIndex) {
        if (input.extent() != ngramSize || data.extent() != ngramSize) {
            return;
        }

        double[] distances = new double[3];
        boolean[] distanceValid = new boolean[3];

        for (int i = 0; i < 3; i++) {
            distances[i] = distance(input.at(i), data.at(i));
            distanceValid[i] = distances[i] >= acceptanceThreshold;
        }

        Prediction prediction = mutablePredictions.get(predictionIndex);
        if (distanceValid[0] && distanceValid[1] && distanceValid[2]) {
            prediction.addSuggestionTriGram(new Suggestion(distances[1], data.at(1)));
        } else if ((distanceValid[0] || distanceValid[2]) && distanceValid[1]) {
            prediction.addSuggestionBiGram(new Suggestion(distances[1], data.at(1)));
        } else if (distanceValid[1] && !distanceValid[0] && !distanceValid[2]) {
            prediction.addSuggestionDirect(new Suggestion(distances[1], data.at(1)));
        }
    }

    private static Double distance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return -1.0;
        }
        return word1.similares(word2, Legacy.Similitude.Cosine);
    }
}