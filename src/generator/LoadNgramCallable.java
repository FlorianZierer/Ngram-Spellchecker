package generator;

import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;
import util.FileUtils;
import util.PredictionUtils;

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
    private final int ngramSize;
    private final List<Prediction> mutablePredictions;
    private final int threadID;
    private final int startIndex;
    private final int endIndex;

    public LoadNgramCallable(Path jsonFilePath, Texture<Prediction> predictions, Texture<Script> paddedWords, int threadID, int ngrams, double acceptanceThreshold, int startIndex, int endIndex) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = paddedWords.grammy(ngrams);
        this.acceptanceThreshold = acceptanceThreshold;
        this.ngramSize = ngrams;
        this.mutablePredictions = new ArrayList<>(predictions.toList());
        this.threadID = threadID;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        loadExistingNgrams();
        return new Texture<>(mutablePredictions);
    }

    private void loadExistingNgrams() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            String line;
            int currentIndex = 0;
            StringBuilder batchBuilder = new StringBuilder("[");

            while ((line = reader.readLine()) != null && currentIndex < endIndex) {
                if (currentIndex >= startIndex) {
                    if (batchBuilder.length() > 1) {
                        batchBuilder.append(",");
                    }
                    batchBuilder.append(line);

                    if (batchBuilder.length() > 1000000) { // Process in chunks of approximately 1MB
                        batchBuilder.append("]");
                        processNgramBatch(batchBuilder.toString());
                        batchBuilder = new StringBuilder("[");
                    }
                }
                currentIndex++;
            }

            if (batchBuilder.length() > 1) {
                batchBuilder.append("]");
                processNgramBatch(batchBuilder.toString());
            }
        }
    }

    private void processNgramBatch(String jsonBatch) {
        try {
            Nexus.DataNote ngramNote = Nexus.DataNote.byJSON(jsonBatch);
            Texture<Script> ngram = new Texture<>(ngramNote.asList(inner -> new Script(inner.asString())));
            filterForSuggestions(ngram);
        } catch (Exception e) {
            System.err.println("Thread " + threadID + ": Error processing JSON batch: " + e.getMessage());
        }
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