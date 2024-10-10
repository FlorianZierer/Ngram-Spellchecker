package core;

import constants.Constants;
import generator.NgramGenerator;
import generator.PredictionGenerator;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.FileUtils;
import util.PredictionUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// Klasse zur Durchführung von Rechtschreibprüfungen
public class SpellChecker {
    private final int nGramLength = 3; // Länge der zu verwendenden N-Gramme
    private final double acceptanceThreshold; // Schwellenwert für die Akzeptanz von Vorschlägen
    private List<Path> jsonFolders; // Liste der Ordner mit JSON-Dateien
    private int createThreads; // Anzahl der zu erstellenden Threads
    private final int epochs; // Anzahl der Durchläufe

    // Konstruktor für den SpellChecker
    public SpellChecker(double acceptanceThreshold, int threads, int epochs) {
        this.acceptanceThreshold = acceptanceThreshold;
        this.createThreads = threads;
        this.epochs = epochs;
    }

    // Methode zum Setzen der Korpora und Generieren von N-Grammen
    public void setCorpora(Path directoryPath, double percent) {
        try {
            NgramGenerator.generateNgrams(directoryPath, nGramLength, createThreads, percent, epochs);
            this.jsonFolders = FileUtils.getJsonFolders(directoryPath.resolve("Json"));
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Methode zum Erhalten von Vorhersagen für gegebene Wörter
    public Texture<Prediction> getPredictions(Texture<Script> searchForWords, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        List<Prediction> allPredictions = searchForWords.stream()
                .map(word -> new Prediction(word, directModeEnabled))
                .collect(Collectors.toList());

        for (Path jsonFolder : jsonFolders) {
            List<Path> jsonFilePaths = FileUtils.getJsonFiles(jsonFolder);
            Texture<Prediction> filePredictions = PredictionGenerator.generatePredictions(jsonFilePaths, searchForWords, nGramLength, acceptanceThreshold, directModeEnabled);

            // Zusammenführen der Vorhersagen aus dieser Datei mit bestehenden Vorhersagen
            for (int i = 0; i < allPredictions.size(); i++) {
                Prediction existingPrediction = allPredictions.get(i);
                Prediction newPrediction = filePredictions.at(i);

                existingPrediction.getSuggestionsTriGram().addAll(newPrediction.getSuggestionsTriGram());
                existingPrediction.getSuggestionsBiGram().addAll(newPrediction.getSuggestionsBiGram());
                existingPrediction.getSuggestionsDirect().addAll(newPrediction.getSuggestionsDirect());
            }
        }
        return new Texture<>(allPredictions);
    }
}