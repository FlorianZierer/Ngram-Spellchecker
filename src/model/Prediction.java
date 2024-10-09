package model;

import lingologs.Script;
import lingologs.Texture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// Klasse zur Handhabung von Vorhersagen basierend auf verschiedenen Suggestion-Typen
public class Prediction {
    private Script word;
    private Texture<Suggestion> suggestionsTriGram;
    private Texture<Suggestion> suggestionsBiGram;
    private Texture<Suggestion> suggestionsDirect;
    private static final double IGNORE_THRESHOLD = 0.90;
    private static final double PERFECT_SCORE = 1.0;
    private boolean directModeEnabled;

    // Konstruktor für die Prediction-Klasse
    public Prediction(Script word, boolean directModeEnabled) {
        this.word = word;
        this.suggestionsTriGram = new Texture<>();
        this.suggestionsBiGram = new Texture<>();
        this.suggestionsDirect = new Texture<>();
        this.directModeEnabled = directModeEnabled;
    }

    // Methode zur Ermittlung der besten Vorhersage basierend auf den verfügbaren Suggestions
    public Script getPrediction() {
        if (directModeEnabled) {
            // Logik für den direkten Modus
            if (suggestionsDirect != null && !suggestionsDirect.isEmpty()) {
                Suggestion directSuggestion = suggestionsDirect.at(0);
                if (directSuggestion.getDistance() >= IGNORE_THRESHOLD) {
                    return directSuggestion.getScript();
                }
            }
            return word;
        }

        // Logik für den nicht-direkten Modus
        if (suggestionsTriGram != null && !suggestionsTriGram.isEmpty()) {
            Suggestion triGramSuggestion = suggestionsTriGram.at(0);
            if (triGramSuggestion.getDistance() >= IGNORE_THRESHOLD) {
                return triGramSuggestion.getScript();
            }
        }

        if (suggestionsBiGram != null && !suggestionsBiGram.isEmpty()) {
            Suggestion biGramSuggestion = suggestionsBiGram.at(0);
            if (biGramSuggestion.getDistance() >= IGNORE_THRESHOLD) {
                return biGramSuggestion.getScript();
            }
        }

        if (suggestionsDirect != null && !suggestionsDirect.isEmpty()) {
            Suggestion directSuggestion = suggestionsDirect.at(0);
            if (directSuggestion.getDistance() == PERFECT_SCORE) {
                return directSuggestion.getScript();
            }
        }

        return word;
    }

    // Methode zum Zusammenführen von zwei Suggestion-Listen
    private Texture<Suggestion> mergeSuggestions(Texture<Suggestion> suggestions1, Texture<Suggestion> suggestions2) {
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

    // Methode zum Sortieren der Suggestions basierend auf Distanz und Wiederholungsanzahl
    public void sort() {
        Comparator<Suggestion> suggestionComparator = (s1, s2) -> {
            int distanceCompare = Double.compare(s2.getDistance(), s1.getDistance());
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Integer.compare(s2.getRepetitionCount(), s1.getRepetitionCount());
        };

        if (suggestionsTriGram != null) {
            this.suggestionsTriGram = new Texture<>(suggestionsTriGram.toList().stream()
                    .sorted(suggestionComparator)
                    .toList());
        }

        if (suggestionsBiGram != null) {
            this.suggestionsBiGram = new Texture<>(suggestionsBiGram.toList().stream()
                    .sorted(suggestionComparator)
                    .toList());
        }

        if (suggestionsDirect != null) {
            this.suggestionsDirect = new Texture<>(suggestionsDirect.toList().stream()
                    .sorted(suggestionComparator)
                    .toList());
        }
    }

    // Methoden zum Hinzufügen von Suggestions zu den verschiedenen Listen

    public void addSuggestionTriGram(Suggestion suggestion) {
        if (suggestionsTriGram == null) {
            suggestionsTriGram = new Texture<>();
        }
        addSuggestion(suggestion, suggestionsTriGram, s -> this.suggestionsTriGram = s);
    }

    public void addSuggestionBiGram(Suggestion suggestion) {
        if (suggestionsBiGram == null) {
            suggestionsBiGram = new Texture<>();
        }
        addSuggestion(suggestion, suggestionsBiGram, s -> this.suggestionsBiGram = s);
    }

    public void addSuggestionDirect(Suggestion suggestion) {
        if (suggestionsDirect == null) {
            suggestionsDirect = new Texture<>();
        }
        addSuggestion(suggestion, suggestionsDirect, s -> this.suggestionsDirect = s);
    }

    // Hilfsmethode zum Hinzufügen einer Suggestion zu einer Liste
    private void addSuggestion(Suggestion suggestion, Texture<Suggestion> suggestions, java.util.function.Consumer<Texture<Suggestion>> setter) {
        boolean found = false;
        for (Suggestion s : suggestions) {
            if (suggestion.getScript().equals(s.getScript())) {
                s.incrementRepetitionCount();
                found = true;
                break;
            }
        }
        if (!found) {
            suggestion.incrementRepetitionCount(); // Increment for new suggestions too
            setter.accept(suggestions.add(suggestion));
        }
    }

    // Getter- und Setter-Methoden

    public Texture<Suggestion> getSuggestionsTriGram() {
        return suggestionsTriGram;
    }

    public void setSuggestionsTriGram(Texture<Suggestion> suggestionsTriGram) {
        this.suggestionsTriGram = suggestionsTriGram;
    }

    public Texture<Suggestion> getSuggestionsBiGram() {
        return suggestionsBiGram;
    }

    public void setSuggestionsBiGram(Texture<Suggestion> suggestionsBiGram) {
        this.suggestionsBiGram = suggestionsBiGram;
    }

    public Texture<Suggestion> getSuggestionsDirect() {
        return suggestionsDirect;
    }

    public void setSuggestionsDirect(Texture<Suggestion> suggestionsDirect) {
        this.suggestionsDirect = suggestionsDirect;
    }

    public Script getWord() {
        return word;
    }

    public void setWord(Script word) {
        this.word = word;
    }

    public boolean isDirectModeEnabled() {
        return directModeEnabled;
    }

    public void setDirectModeEnabled(boolean directModeEnabled) {
        this.directModeEnabled = directModeEnabled;
    }
}