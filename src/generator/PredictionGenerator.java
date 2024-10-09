package generator;

import lingolava.Tuple.Couple;
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
    public static Texture<Prediction> generatePredictions(List<Path> jsonFiles, Texture<Script> searchForWords,int epochs, int nGramLength, double acceptanceThreshold, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        // Hinzufügen von Polsterung zu den Suchwörtern
        Texture<Script> paddedWords = addPadding(searchForWords);
        // Erstellen initialer Vorhersagen
        Texture<Prediction> predictions = new Texture<>(searchForWords.map(word -> new Prediction(word, directModeEnabled)).toList());

        int threads = jsonFiles.size();
        List<Integer> ngramsPerThreads = new ArrayList<>();
        for (Path jsonFile : jsonFiles) {
            int totalSentences = FileUtils.countSentences(String.valueOf(jsonFile));
            int ngramsPerThread = totalSentences / threads;
            ngramsPerThreads.add(ngramsPerThread);
        }

        for(int epoch = 0; epoch < epochs; epoch++) {
            predictionBuilder.attach(predictEpochMultiThreaded(jsonFiles, ngramsPerThreads, predictions, paddedWords ,threads, nGramLength, acceptanceThreshold,epoch));
        }
        return PredictionUtils.deduplicateAndSortPredictions(predictionBuilder.toTexture());
    }

    // Hauptmethode zur Generierung von Vorhersagen
    public static Texture<Prediction> predictEpochMultiThreaded(List<Path> jsonFiles, List<Integer> ngramsPerThreads, Texture<Prediction> predictions, Texture<Script> paddedWords ,int threads, int ngrams, double acceptanceThreshold,int epoch) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        List<LoadNgramCallable> ngramCallables = new ArrayList<>();
        int thread = 0;
        for (Path jsonFile : jsonFiles) {
            int startNgramIndex = epoch * ngramsPerThreads.get(thread);
            int endNgramIndex = startNgramIndex + ngramsPerThreads.get(thread);
            ngramCallables.add(new LoadNgramCallable(jsonFile, predictions, paddedWords,thread, ngrams, acceptanceThreshold));
            thread++;
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
        return predictionBuilder.toTexture();
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