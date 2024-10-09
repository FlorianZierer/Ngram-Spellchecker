package generator;

import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.FileUtils;
import util.PredictionUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PredictionGenerator {
    // Hauptmethode zur Generierung von Vorhersagen
    public static Texture<Prediction> generatePredictions(Path jsonFile, Texture<Script> searchForWords, int threads, int ngrams, double acceptanceThreshold, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        // Hinzufügen von Polsterung zu den Suchwörtern
        Texture<Script> paddedWords = addPadding(searchForWords);

        // Erstellen initialer Vorhersagen
        Texture<Prediction> predictions = new Texture<>(searchForWords.map(word -> new Prediction(word, directModeEnabled)).toList());

        // Berechnung der N-Gramme pro Thread
        int totalNgrams = FileUtils.countTotalNgrams(jsonFile);
        int ngramsPerThread = totalNgrams / threads;

        // Erstellen von Callable-Objekten für jeden Thread
        List<LoadNgramCallable> ngramCallables = new ArrayList<>();
        for (int threadID = 0; threadID < threads; threadID++) {
            int startNgramIndex = ngramsPerThread * threadID;
            int endNgramIndex = (threadID == threads - 1) ? totalNgrams : startNgramIndex + ngramsPerThread;
            ngramCallables.add(new LoadNgramCallable(jsonFile, predictions, paddedWords, ngrams, acceptanceThreshold, startNgramIndex, endNgramIndex));
        }

        // Ausführen der Threads und Sammeln der Ergebnisse
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Future<Texture<Prediction>>> futures = ngramCallables.stream()
                .map(executorService::submit)
                .toList();

        for (Future<Texture<Prediction>> future : futures) {
            try {
                predictionBuilder.attach(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        executorService.shutdown();

        // Entfernen von Duplikaten und Sortieren der Vorhersagen
        return PredictionUtils.deduplicateAndSortPredictions(predictionBuilder.toTexture());
    }

    // Methode zum Hinzufügen von Polsterung zu den Suchwörtern
    private static Texture<Script> addPadding(Texture<Script> wordsToSearch) {
        Texture.Builder<Script> paddedWords = new Texture.Builder<>();
        paddedWords.attach(Script.of(""));
        paddedWords.attach(wordsToSearch);
        paddedWords.attach(Script.of(""));
        return paddedWords.toTexture();
    }
}