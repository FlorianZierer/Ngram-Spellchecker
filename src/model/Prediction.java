package model;

import lingologs.Script;

import java.util.*;
import java.util.stream.Collectors;

public class Prediction {
    private Script word;
    private List<Suggestion> suggestionsTriGram;
    private List<Suggestion> suggestionsBiGram;
    private List<Suggestion> suggestionsDirect;
    private static final double IGNORE_PERFECT_SCORE_THRESHOLD = 0.90;
    private static final double PERFECT_SCORE = 0.99;
    private boolean directModeEnabled;

    public Prediction(Script word, boolean directModeEnabled) {
        this.word = word;
        this.suggestionsTriGram = new ArrayList<>();
        this.suggestionsBiGram = new ArrayList<>();
        this.suggestionsDirect = new ArrayList<>();
        this.directModeEnabled = directModeEnabled;
    }

    public Script getPrediction() {
        // Direct Mode - Keine Hierarchie
        if (directModeEnabled) {
            if (!suggestionsDirect.isEmpty()) {
                return suggestionsDirect.getFirst().getScript();
            }
            return word; // Fallback auf das Originalwort
        }

        // 1. Prüfe auf IGNORE_PERFECT_SCORE_THRESHOLD in der Hierarchie
        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.getFirst().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsTriGram.getFirst().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.getFirst().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsBiGram.getFirst().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.getFirst().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsDirect.getFirst().getScript();
        }

        // 2. Prüfe auf Perfect Score in der Hierarchie
        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.getFirst().getDistance() == PERFECT_SCORE) {
            return suggestionsTriGram.getFirst().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.getFirst().getDistance() == PERFECT_SCORE) {
            return suggestionsBiGram.getFirst().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.getFirst().getDistance() == PERFECT_SCORE) {
            return suggestionsDirect.getFirst().getScript();
        }

        // 3. Durchlaufe die Hierarchie ohne weitere Bedingungen
        if (!suggestionsTriGram.isEmpty()) {
            return suggestionsTriGram.getFirst().getScript();
        }
        if (!suggestionsBiGram.isEmpty()) {
            return suggestionsBiGram.getFirst().getScript();
        }
        if (!suggestionsDirect.isEmpty()) {
            return suggestionsDirect.getFirst().getScript();
        }

        // Fallback auf das Originalwort, wenn keine Vorschläge vorhanden sind
        return word;
    }

    public void reduceAllLists() {
        reduceList(suggestionsTriGram);
        reduceList(suggestionsBiGram);
        reduceList(suggestionsDirect);
        sortLists();
    }

    public void reduceList(List<Suggestion> suggestions) {
        List<Suggestion> reducedList = suggestions.stream()
                .collect(Collectors.groupingBy(Suggestion::getScript,
                        Collectors.reducing(Suggestion::merge)))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        suggestions.clear();
        suggestions.addAll(reducedList);
    }

    public void sortLists() {
        Comparator<Suggestion> suggestionComparator = (s1, s2) -> {
            int distanceCompare = Double.compare(s2.getDistance(), s1.getDistance());
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Integer.compare(s2.getRepetitionCount(), s1.getRepetitionCount());
        };

        suggestionsTriGram.sort(suggestionComparator);
        suggestionsBiGram.sort(suggestionComparator);
        suggestionsDirect.sort(suggestionComparator);
    }

    public void addSuggestionTriGram(Suggestion suggestion) {
        this.suggestionsTriGram.add(suggestion);
    }

    public void addSuggestionBiGram(Suggestion suggestion) {
        this.suggestionsBiGram.add(suggestion);
    }

    public void addSuggestionDirect(Suggestion suggestion) {
        this.suggestionsDirect.add(suggestion);
    }

    public List<Suggestion> getSuggestionsTriGram() {
        return suggestionsTriGram;
    }

    public void setSuggestionsTriGram(List<Suggestion> suggestionsTriGram) {
        this.suggestionsTriGram = suggestionsTriGram;
    }

    public List<Suggestion> getSuggestionsBiGram() {
        return suggestionsBiGram;
    }

    public void setSuggestionsBiGram(List<Suggestion> suggestionsBiGram) {
        this.suggestionsBiGram = suggestionsBiGram;
    }

    public List<Suggestion> getSuggestionsDirect() {
        return suggestionsDirect;
    }

    public void setSuggestionsDirect(List<Suggestion> suggestionsDirect) {
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