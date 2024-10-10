package generator;

import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;

import static util.PredictionUtils.distance;

public class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private static final Logger LOGGER = Logger.getLogger(LoadNgramCallable.class.getName());

    private final List<Texture<Script>> ngramsToSearch;
    private final double acceptanceThreshold;
    private List<Prediction> mutablePredictions;
    private final int epoch;
    private final String[] jsonArray;

    public LoadNgramCallable(String[] jsonArray, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int epoch) {
        this.jsonArray = jsonArray;
        this.ngramsToSearch = paddedWords.grammy(ngrams);
        this.acceptanceThreshold = acceptanceThreshold;
        this.mutablePredictions = predictions.toList();
        this.epoch = epoch;
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        try {
            processNgramBatch(jsonArray[epoch]);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in call method", e);
        }
        return new Texture<>(mutablePredictions);
    }

    private void processNgramBatch(String jsonBatch) {
        try {
            Nexus.DataNote ngramNote = Nexus.DataNote.byJSON(jsonBatch);
            Texture<Texture<Script>> loadedNgrams = new Texture<>(ngramNote.asList(d -> new Texture<>(d.asList(inner -> new Script(inner.asString())))));
            loadedNgrams.forEach(this::filterForSuggestions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing ngram batch", e);
        }
    }

    private void filterForSuggestions(Texture<Script> inputNgram) {
        if (inputNgram == null || inputNgram.extent() == 0) {
            LOGGER.warning("Null or empty inputNgram encountered");
            return;
        }

        int size = ngramsToSearch.size();
        for (int i = 0; i < size; i++) {
            try {
                getSuggestion(ngramsToSearch.get(i), inputNgram, i);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing suggestion for index " + i, e);
            }
        }
    }

    private void getSuggestion(Texture<Script> input, Texture<Script> data, int predictionIndex) {
        try {
            if (input == null || data == null || input.extent() < 3 || data.extent() < 3) {
                LOGGER.warning("Invalid input or data in getSuggestion: input=" + input + ", data=" + data);
                return;
            }

            double distance1 = distance(input.at(0), data.at(0));
            double distance2 = distance(input.at(1), data.at(1));
            double distance3 = distance(input.at(2), data.at(2));

            boolean distance1Valid = distance1 >= acceptanceThreshold;
            boolean distance2Valid = distance2 >= acceptanceThreshold;
            boolean distance3Valid = distance3 >= acceptanceThreshold;

            Prediction prediction = mutablePredictions.get(predictionIndex);
            if (prediction == null) {
                LOGGER.warning("Null prediction at index " + predictionIndex);
                return;
            }

            if (distance1Valid && distance2Valid && distance3Valid) {
                prediction.addSuggestionTriGram(new Suggestion(distance2, data.at(1)));
            } else if ((distance1Valid || distance3Valid) && distance2Valid) {
                prediction.addSuggestionBiGram(new Suggestion(distance2, data.at(1)));
            } else if (distance2Valid) {
                prediction.addSuggestionDirect(new Suggestion(distance2, data.at(1)));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in getSuggestion method", e);
        }
    }
}