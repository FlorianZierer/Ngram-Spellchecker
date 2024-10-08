import lingologs.Script;
import lingologs.Texture;

public class Prediction {
    private Script word;

    private Texture<Suggestion> suggestionsTriGram = new Texture<>();
    private Texture<Suggestion> suggestionsBiGram = new Texture<>();
    private Texture<Suggestion> suggestionsDirect = new Texture<>();

    public Prediction(Script word) {
    this.word = word;
    }



    public void addSuggestionTriGram(Suggestion suggestion) {
        for (Suggestion s : suggestionsTriGram) {
            if (suggestion.equals(s)) {
                suggestion.incrementRepetitionCount();
            } else {
                suggestionsTriGram.add(suggestion);
            }

        }
    }
    public void addSuggestionBiGram(Suggestion suggestion) {
        for (Suggestion s : suggestionsBiGram) {
            if (suggestion.equals(s)) {
                suggestion.incrementRepetitionCount();
            } else {
                suggestionsBiGram.add(suggestion);
            }

        }
    }
    public void addSuggestionDirect(Suggestion suggestion) {
        for (Suggestion s : suggestionsDirect) {
            if (suggestion.equals(s)) {
                suggestion.incrementRepetitionCount();
            } else {
                suggestionsDirect.add(suggestion);
            }

        }
    }

    public int getMostFrequentWord() {
        int trigram = suggestionsTriGram.stream()
                .map(Suggestion::getRepetitionCount)
                .max(Integer::compareTo)
                .orElse(0);

        int bigram = suggestionsBiGram.stream()
                .map(Suggestion::getRepetitionCount)
                .max(Integer::compareTo)
                .orElse(0);

        int direct = suggestionsDirect.stream()
                .map(Suggestion::getRepetitionCount)
                .max(Integer::compareTo)
                .orElse(0);

        return Math.max(trigram, Math.max(bigram, direct));
    }

    public Texture<Suggestion> getSuggestionsDirect() {
        return suggestionsDirect;
    }

    public void setSuggestionsDirect(Texture<Suggestion> suggestionsDirect) {
        this.suggestionsDirect = suggestionsDirect;
    }
}