package util;

import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PredictionUtils {
    public static Texture<Prediction> deduplicateAndSortPredictions(Texture<Prediction> predictions) {
        List<Script> predictionsNames = new ArrayList<>();
        List<Prediction> condensedList = new ArrayList<>();

        for (Prediction prediction : predictions) {
            if (!(predictionsNames.contains(prediction.getWord()))) {
                condensedList.add(prediction);
                predictionsNames.add(prediction.getWord());
            } else {
                for (Prediction existingPrediction : condensedList) {
                    if (existingPrediction.getWord().equals(prediction.getWord())) {
                        existingPrediction.setSuggestionsTriGram(
                                mergeSuggestions(existingPrediction.getSuggestionsTriGram(), prediction.getSuggestionsTriGram()));
                        existingPrediction.setSuggestionsBiGram(
                                mergeSuggestions(existingPrediction.getSuggestionsBiGram(), prediction.getSuggestionsBiGram()));
                        existingPrediction.setSuggestionsDirect(
                                mergeSuggestions(existingPrediction.getSuggestionsDirect(), prediction.getSuggestionsDirect()));
                        break;
                    }
                }
            }
        }

        condensedList.forEach(Prediction::sort);
        return new Texture<>(condensedList);
    }

    public static Texture<Suggestion> mergeSuggestions(Texture<Suggestion> suggestions1, Texture<Suggestion> suggestions2) {
        List<Suggestion> mergedList = new ArrayList<>();

        if (suggestions1 != null) {
            mergedList.addAll(suggestions1.toList());
        }

        if (suggestions2 != null) {
            for (Suggestion suggestion : suggestions2) {
                boolean found = false;
                for (Suggestion existingSuggestion : mergedList) {
                    if (Objects.equals(suggestion.getScript(), existingSuggestion.getScript())) {
                        existingSuggestion.merge(suggestion);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    mergedList.add(suggestion);
                }
            }
        }
        return new Texture<>(mergedList);
    }
}