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

// Diese Klasse implementiert einen Rechtschreibprüfer basierend auf N-Gramm-Modellen
public class SpellChecker {
    private final int nGramLength = 3; // fixed weil Predictions nur mit 3 Funktionieren. Generierung hätte damit kein Problem.
    private final double acceptanceThreshold;
    private List<Path> jsonFolders;
    private int createThreads;

    // Konstruktor, der den Akzeptanzschwellenwert für Vorhersagen setzt
    public SpellChecker(double acceptanceThreshold, int threads) {
        this.acceptanceThreshold = acceptanceThreshold;
        this.createThreads = threads;
    }

    // Methode zum Setzen und Generieren der Korpora für die Rechtschreibprüfung
    public void setCorpora(Path directoryPath, double percent, int epochs) {
        try {
            // Generiert N-Gramme aus den Trainingsdaten
            NgramGenerator.generateNgrams(directoryPath, nGramLength, createThreads, percent, epochs);
            // Speichert die Pfade zu den generierten JSON-Dateien
            this.jsonFolders = FileUtils.getJsonFolders(directoryPath.resolve("Json"));
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Methode zur Generierung von Vorhersagen für gegebene Wörter
    public Texture<Prediction> getPredictions(Texture<Script> searchForWords, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> allPredictions = new Texture.Builder<>();

        // Durchsucht alle JSON-Dateien nach Vorhersagen
        for (Path jsonFolder : jsonFolders) {
            int threads = FileUtils.getJsonFiles(jsonFolder).size();
            List<Path> jsonFilePaths = FileUtils.getJsonFiles(jsonFolder);
                //System.out.println(Constants.ANSI_RESET + "Searching json file: " + jsonFile.getFileName().toString() + Constants.ANSI_RESET );

                // Generiert Vorhersagen für die aktuelle JSON-Datei
                Texture<Prediction> filePredictions = PredictionGenerator.generatePredictions(jsonFilePaths, searchForWords, threads, nGramLength, acceptanceThreshold, directModeEnabled);
                allPredictions.attach(filePredictions);
        }

        // Entfernt Duplikate und sortiert die Vorhersagen
        return PredictionUtils.deduplicateAndSortPredictions(allPredictions.toTexture());
    }
}