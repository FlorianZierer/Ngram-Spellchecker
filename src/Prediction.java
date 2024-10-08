import lingologs.Script;
import lingologs.Texture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Prediction {
    private Script word;

    private Texture<Suggestion> suggestionsTriGram;
    private Texture<Suggestion> suggestionsBiGram;
    private Texture<Suggestion> suggestionsDirect;

    public Prediction(Script word) {
        this.word = word;
        this.suggestionsTriGram = new Texture<>();
        this.suggestionsBiGram = new Texture<>();
        this.suggestionsDirect = new Texture<>();
    }

    public Prediction() {}

    public Prediction(Prediction prediction1, Prediction prediction2) {
        // Check if either prediction is null or has a null word
        if (prediction1 == null || prediction2 == null ||
                prediction1.getWord() == null || prediction2.getWord() == null) {
            return;
        }

        // Check if words are equal, considering null cases
        if (!Objects.equals(prediction1.getWord(), prediction2.getWord())) {
            return;
        }

        this.word = prediction1.getWord(); // Set the word

        // Merge TriGram suggestions
        Texture<Suggestion> mergedTriGram = mergeSuggestions(prediction1.getSuggestionsTriGram(), prediction2.getSuggestionsTriGram());
        setSuggestionsTriGram(mergedTriGram);

        // Merge BiGram suggestions
        Texture<Suggestion> mergedBiGram = mergeSuggestions(prediction1.getSuggestionsBiGram(), prediction2.getSuggestionsBiGram());
        setSuggestionsBiGram(mergedBiGram);

        // Merge Direct suggestions
        Texture<Suggestion> mergedDirect = mergeSuggestions(prediction1.getSuggestionsDirect(), prediction2.getSuggestionsDirect());
        setSuggestionsDirect(mergedDirect);
    }

    public Script getPrediction() {
        if (suggestionsTriGram != null && !suggestionsTriGram.isEmpty()) {
            return this.suggestionsTriGram.at(0).getScript();
        }
        if (suggestionsBiGram != null && !suggestionsBiGram.isEmpty()) {
            return this.suggestionsBiGram.at(0).getScript();
        }
        if (suggestionsDirect != null && !suggestionsDirect.isEmpty()) {
            return this.suggestionsDirect.at(0).getScript();
        }
        return word;
    }

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
}