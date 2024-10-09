package util;

import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Klasse für Vorhersage-Operationen
public class PredictionUtils {
    // Methode zum Deduplizieren und Sortieren von Vorhersagen
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
                        // Zusammenführen von Vorschlägen für doppelte Vorhersagen
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

        // Sortieren der Vorhersagen
        condensedList.forEach(Prediction::sort);
        return new Texture<>(condensedList);
    }

    // Quelle die beim verstehen geholfen hat: https://datascience.stackexchange.com/questions/63325/cosine-similarity-vs-the-levenshtein-distance
    // Ich habe Gpt gefragt, ob eine kombinierte Nutzung Sinn macht: Das kombinierte Verwenden von Levenshtein- und Cosinus-Distanz ist sinnvoll, da Levenshtein Zeichenunterschiede misst und Cosinus-Distanz semantische Ähnlichkeit zwischen Texten erfasst. So wird sowohl die strukturelle als auch die inhaltliche Übereinstimmung bewertet.
    // Berechnet die Distanz zwischen zwei Wörtern


    public static double distance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return 0.0;
        }
        // Berechne Cosine-Ähnlichkeit
        double cosineSimularity = word1.similares(word2, Legacy.Similitude.Cosine);
        // Berechne normalisierte Levenshtein-Distanz
        double levenshteinDistance = word1.similares(word2, Legacy.Similitude.Levenshtein);
        // Gewichteter Durchschnitt von Cosine-Ähnlichkeit und normalisierter Levenshtein-Distanz
        return (0.5 * cosineSimularity) + (0.5 * levenshteinDistance);
    }

    // Methode zum Zusammenführen von Vorschlägen
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