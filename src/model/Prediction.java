package model;

import lingologs.Script;

public class Prediction {
    private Script word;
    private SuggestionSet suggestionsTriGram;
    private SuggestionSet suggestionsBiGram;
    private SuggestionSet suggestionsDirect;
    private static final double IGNORE_PERFECT_SCORE_THRESHOLD = 0.90;
    private static final double PERFECT_SCORE = 0.99;
    private boolean directModeEnabled;
    private Script cachedPrediction;

    public Prediction(Script word, boolean directModeEnabled) {
        this.word = word;
        this.suggestionsTriGram = new SuggestionSet();
        this.suggestionsBiGram = new SuggestionSet();
        this.suggestionsDirect = new SuggestionSet();
        this.directModeEnabled = directModeEnabled;
    }

    public Script getPrediction() {
        if (cachedPrediction == null) {
            cachedPrediction = computePrediction();
        }
        return cachedPrediction;
    }

    private Script computePrediction() {
        if (directModeEnabled) {
            return suggestionsDirect.isEmpty() ? word : suggestionsDirect.first().getScript();
        }

        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsTriGram.first().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsBiGram.first().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsDirect.first().getScript();
        }

        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.first().getDistance() == PERFECT_SCORE) {
            return suggestionsTriGram.first().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.first().getDistance() == PERFECT_SCORE) {
            return suggestionsBiGram.first().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.first().getDistance() == PERFECT_SCORE) {
            return suggestionsDirect.first().getScript();
        }

        if (!suggestionsTriGram.isEmpty()) return suggestionsTriGram.first().getScript();
        if (!suggestionsBiGram.isEmpty()) return suggestionsBiGram.first().getScript();
        if (!suggestionsDirect.isEmpty()) return suggestionsDirect.first().getScript();

        return word;
    }

    public void addSuggestionTriGram(Suggestion suggestion) {
        this.suggestionsTriGram.add(suggestion);
        this.cachedPrediction = null; // Invalidate cache
    }

    public void addSuggestionBiGram(Suggestion suggestion) {
        this.suggestionsBiGram.add(suggestion);
        this.cachedPrediction = null; // Invalidate cache
    }

    public void addSuggestionDirect(Suggestion suggestion) {
        this.suggestionsDirect.add(suggestion);
        this.cachedPrediction = null; // Invalidate cache
    }

    // Getters and setters...

    public SuggestionSet getSuggestionsTriGram() {
        return suggestionsTriGram;
    }

    public SuggestionSet getSuggestionsBiGram() {
        return suggestionsBiGram;
    }

    public SuggestionSet getSuggestionsDirect() {
        return suggestionsDirect;
    }
}