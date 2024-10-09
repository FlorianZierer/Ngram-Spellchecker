package generator;

import constants.Constants;
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
    public static Texture<Prediction> generatePredictions(List<Path> jsonFiles, Texture<Script> searchForWords, int epochs, int nGramLength, double acceptanceThreshold, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        Texture<Script> paddedWords = addPadding(searchForWords);
        Texture<Prediction> predictions = new Texture<>(searchForWords.map(word -> new Prediction(word, directModeEnabled)).toList());

        List<Integer> batchSizes = new ArrayList<>();
        for (Path jsonFile : jsonFiles) {
            int totalSentences = FileUtils.countSentences(String.valueOf(jsonFile));
            int batchSize = (int) Math.ceil((double) totalSentences / epochs);
            batchSizes.add(batchSize);
        }

        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println(Constants.ANSI_RESET + "Searching json folder: " + jsonFiles.getFirst().getParent().toString() + " in epoch: " + epoch +  Constants.ANSI_RESET );
            predictionBuilder.attach(predictEpochMultiThreaded(jsonFiles, batchSizes, predictions, paddedWords, nGramLength, acceptanceThreshold, epoch));
        }
        return predictionBuilder.toTexture();
    }

    // Hauptmethode zur Generierung von Vorhersagen
    public static Texture<Prediction> predictEpochMultiThreaded(List<Path> jsonFiles, List<Integer> batchSizes, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int epoch) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        List<LoadNgramCallable> ngramCallables = new ArrayList<>();
        for (int i = 0; i < jsonFiles.size(); i++) {
            Path jsonFile = jsonFiles.get(i);
            int batchSize = batchSizes.get(i);
            int startIndex = epoch * batchSize;
            int endIndex = Math.min((epoch + 1) * batchSize, FileUtils.countSentences(String.valueOf(jsonFile)));
            ngramCallables.add(new LoadNgramCallable(jsonFile, predictions, paddedWords, i, ngrams, acceptanceThreshold, startIndex, endIndex));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(jsonFiles.size());
        List<Future<Texture<Prediction>>> futures = executorService.invokeAll(ngramCallables);

        for (Future<Texture<Prediction>> future : futures) {
            predictionBuilder.attach(future.get());
        }

        executorService.shutdown();

        return predictionBuilder.toTexture();
    }

    private static Texture<Script> addPadding(Texture<Script> wordsToSearch) {
        Texture.Builder<Script> paddedWords = new Texture.Builder<>();
        paddedWords.attach(Script.of(""));
        paddedWords.attach(wordsToSearch);
        paddedWords.attach(Script.of(""));
        return paddedWords.toTexture();
    }
}