package generator;

import constants.Constants;
import lingolava.Tuple.Couple;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.FileUtils;
import util.PredictionUtils;

import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PredictionGenerator {
    public static Texture<Prediction> generatePredictions(List<Path> jsonFiles, Texture<Script> searchForWords, int nGramLength, double acceptanceThreshold, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        Texture<Script> paddedWords = addPadding(searchForWords);
        Texture<Prediction> predictions = new Texture<>(searchForWords.map(word -> new Prediction(word, directModeEnabled)).toList());

        int epochs = FileUtils.getEpochs(jsonFiles.getFirst());

        long startTime = System.nanoTime();
        Texture.Builder<String[]> jsonListBuilder = new Texture.Builder<>();
        for( Path jsonfile : jsonFiles) {
            String content = Files.readString(jsonfile);
            jsonListBuilder.attach(content.split("###JSON_PART###"));
        }
        long endTime = System.nanoTime();
        long durationInNanos = endTime - startTime;
        double durationInSeconds = durationInNanos / 1_000_000_000.0;

        System.out.println(Constants.ANSI_GREEN + "Gesamtzeit für das Laden aller Json files in ein String Array: " + String.format("%.2f", durationInSeconds) + " Sekunden" + Constants.ANSI_RESET);



        long startTime2 = System.nanoTime();
        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println(Constants.ANSI_BLUE + jsonFiles.getFirst().getParent().toString() + " wird in Epoche " +  epoch + " geladen "  +  Constants.ANSI_RESET );
            predictionBuilder.attach(predictEpochMultiThreaded(jsonListBuilder.toTexture(), predictions, paddedWords, nGramLength, acceptanceThreshold, epoch));
        }
        long endTime2 = System.nanoTime();
        long durationInNanos2 = endTime2 - startTime2;
        double durationInSeconds2 = durationInNanos2 / 1_000_000_000.0;

        System.out.println(Constants.ANSI_GREEN + "Gesamtzeit für alle Epochen laden von Suggestions : " + String.format("%.2f", durationInSeconds2) + " Sekunden" + Constants.ANSI_RESET);
        return PredictionUtils.deduplicateAndSortPredictions(predictionBuilder.toTexture());
    }

    // Hauptmethode zur Generierung von Vorhersagen
    public static Texture<Prediction> predictEpochMultiThreaded(Texture<String[]> jsonList, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int epoch) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();

        List<LoadNgramCallable> ngramCallables = new ArrayList<>();
        for (int i = 0; i < jsonList.extent(); i++) {

            ngramCallables.add(new LoadNgramCallable(jsonList.at(i), predictions, paddedWords, ngrams, acceptanceThreshold, epoch));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(jsonList.extent());
        List<Future<Texture<Prediction>>> futures = executorService.invokeAll(ngramCallables);

        for (Future<Texture<Prediction>> future : futures) {
            predictionBuilder.attach(future.get());
        }

        executorService.shutdown();

        return PredictionUtils.deduplicateAndSortPredictions(predictionBuilder.toTexture());
    }

    private static Texture<Script> addPadding(Texture<Script> wordsToSearch) {
        Texture.Builder<Script> paddedWords = new Texture.Builder<>();
        paddedWords.attach(Script.of(""));
        paddedWords.attach(wordsToSearch);
        paddedWords.attach(Script.of(""));
        return paddedWords.toTexture();
    }
}