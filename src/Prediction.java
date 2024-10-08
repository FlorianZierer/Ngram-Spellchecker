import lingologs.Script;
import lingologs.Texture;

import java.util.Comparator;

public class Prediction {
    private Script word;

    private Texture<Suggestion> suggestionsTriGram = new Texture<>();
    private Texture<Suggestion> suggestionsBiGram = new Texture<>();
    private Texture<Suggestion> suggestionsDirect = new Texture<>();

    public Prediction(Script word) {
    this.word = word;
    }

    public Script getPrediction() {
        if(suggestionsTriGram.extent()>0){
            return suggestionsTriGram.at(0).getScript();
        }
        if(suggestionsBiGram.extent()>0){
            return suggestionsBiGram.at(0).getScript();
        }
        if(suggestionsDirect.extent()>0){
            return suggestionsDirect.at(0).getScript();
        }
        return new Script("NaN");
    }


    public void sort() {
        Comparator<Suggestion> suggestionComparator = (s1, s2) -> {
            int distanceCompare = Double.compare(s2.getScore(), s1.getScore());
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
        for (Suggestion s : suggestionsTriGram) {
            if (suggestion.equals(s)) {
                s.incrementRepetitionCount();
            } else {
                suggestionsTriGram = suggestionsTriGram.add(suggestion);
            }

        }
    }
    public void addSuggestionBiGram(Suggestion suggestion) {
        for (Suggestion s : suggestionsBiGram) {
            if (suggestion.equals(s)) {
                s.incrementRepetitionCount();
            } else {
                suggestionsBiGram = suggestionsBiGram.add(suggestion);
            }

        }
    }
    public void addSuggestionDirect(Suggestion suggestion) {
        for (Suggestion s : suggestionsDirect) {
            if (suggestion.equals(s)) {
                s.incrementRepetitionCount();
            } else {
                suggestionsDirect = suggestionsDirect.add(suggestion);
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