package model;

import lingologs.Script;

public class Prediction {
    // Eigenschaften der Klasse
    private Script word;
    private SuggestionSet suggestionsTriGram;
    private SuggestionSet suggestionsBiGram;
    private SuggestionSet suggestionsDirect;
    private static final double IGNORE_PERFECT_SCORE_THRESHOLD = 0.90;
    private static final double PERFECT_SCORE = 0.99; // oft werden gleiche Worte abgerundet
    private boolean directModeEnabled;
    private Script cachedPrediction;

    // Konstruktor
    public Prediction(Script word, boolean directModeEnabled) {
        this.word = word;
        this.suggestionsTriGram = new SuggestionSet();
        this.suggestionsBiGram = new SuggestionSet();
        this.suggestionsDirect = new SuggestionSet();
        this.directModeEnabled = directModeEnabled;
    }

    // Gibt die Vorhersage zurück, verwendet den Cache wenn möglich
    public Script getPrediction() {
        if (cachedPrediction == null) {
            cachedPrediction = computePrediction();
        }
        return cachedPrediction;
    }

    // Berechnet die Vorhersage basierend auf verschiedenen Kriterien
    private Script computePrediction() {
        // Wenn der direkte Modus aktiviert ist, verwende die direkte Vorschläge
        if (directModeEnabled) {
            return suggestionsDirect.isEmpty() ? word : suggestionsDirect.first().getScript();
        }

        // Prüfe auf hohe Übereinstimmungen in den verschiedenen Vorschlagssets
        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsTriGram.first().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsBiGram.first().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.first().getDistance() >= IGNORE_PERFECT_SCORE_THRESHOLD) {
            return suggestionsDirect.first().getScript();
        }

        // Prüfe auf perfekte Übereinstimmungen
        if (!suggestionsTriGram.isEmpty() && suggestionsTriGram.first().getDistance() == PERFECT_SCORE) {
            return suggestionsTriGram.first().getScript();
        }
        if (!suggestionsBiGram.isEmpty() && suggestionsBiGram.first().getDistance() == PERFECT_SCORE) {
            return suggestionsBiGram.first().getScript();
        }
        if (!suggestionsDirect.isEmpty() && suggestionsDirect.first().getDistance() == PERFECT_SCORE) {
            return suggestionsDirect.first().getScript();
        }

        // Wähle den besten verfügbaren Vorschlag
        if (!suggestionsTriGram.isEmpty()) return suggestionsTriGram.first().getScript();
        if (!suggestionsBiGram.isEmpty()) return suggestionsBiGram.first().getScript();
        if (!suggestionsDirect.isEmpty()) return suggestionsDirect.first().getScript();

        // Wenn keine Vorschläge verfügbar sind, gib das ursprüngliche Wort zurück
        return word;
    }

    // Löscht alle Vorschläge
    public void clear() {
        suggestionsTriGram.clear();
        suggestionsBiGram.clear();
        suggestionsDirect.clear();
    }

    // Fügt einen Tri-Gram-Vorschlag hinzu und invalidiert den Cache
    public void addSuggestionTriGram(Suggestion suggestion) {
        this.suggestionsTriGram.add(suggestion);
        this.cachedPrediction = null; // Cache invalidieren
    }

    // Fügt einen Bi-Gram-Vorschlag hinzu und invalidiert den Cache
    public void addSuggestionBiGram(Suggestion suggestion) {
        this.suggestionsBiGram.add(suggestion);
        this.cachedPrediction = null; // Cache invalidieren
    }

    // Fügt einen direkten Vorschlag hinzu und invalidiert den Cache
    public void addSuggestionDirect(Suggestion suggestion) {
        this.suggestionsDirect.add(suggestion);
        this.cachedPrediction = null; // Cache invalidieren
    }

    // Getter-Methoden

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