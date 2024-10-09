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

    public LoadNgramCallable(Path jsonFilePath, Texture<Prediction> predictions, Texture<Script> paddedWords, int threadID, int ngrams, double acceptanceThreshold) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = paddedWords.grammy(ngrams);
        this.acceptanceThreshold = acceptanceThreshold;
        this.ngramSize = ngrams;
        this.mutablePredictions = new ArrayList<>(predictions.toList());
        this.threadID = threadID;
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        loadExistingNgrams();
        return new Texture<>(mutablePredictions);
    }

    private void loadExistingNgrams() throws IOException {
        int batchSize = FileUtils.calculateBatchSize(jsonFilePath, 1); // Assuming 1 epoch for simplicity
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            List<String> batch = new ArrayList<>();
            String line;
            boolean isFirstBatch = true;

            while ((line = reader.readLine()) != null) {
                batch.add(line);

                if (batch.size() >= batchSize) {
                    processNgramBatch(batch, isFirstBatch);
                    batch.clear();
                    isFirstBatch = false;
                }
            }

            if (!batch.isEmpty()) {
                processNgramBatch(batch, isFirstBatch);
            }
        }
    }

    private void processNgramBatch(List<String> batch, boolean isFirstBatch) throws IOException {
        StringBuilder ngramBuilder = new StringBuilder();
        if (!isFirstBatch) {
            ngramBuilder.append("[");
        }

        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                ngramBuilder.append(",");
            }
            ngramBuilder.append(batch.get(i));
        }

        if (batch.size() < FileUtils.calculateBatchSize(jsonFilePath, 1)) {
            ngramBuilder.append("]");
        } else {
            ngramBuilder.setLength(ngramBuilder.length() - 1);
            ngramBuilder.append("]");
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