package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SuggestionSet {
    private final List<Suggestion> suggestions;

    public SuggestionSet() {
        this.suggestions = new CopyOnWriteArrayList<>();
    }

    public synchronized void add(Suggestion suggestion) {
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion existing = suggestions.get(i);
            if (existing.getScript().equals(suggestion.getScript())) {
                Suggestion newSuggestion = new Suggestion(
                        Math.max(existing.getDistance(), suggestion.getDistance()),
                        existing.getScript(),
                        existing.getRepetitionCount() + suggestion.getRepetitionCount()
                );
                suggestions.set(i, newSuggestion);
                sortSuggestions();
                return;
            }
        }
        suggestions.add(suggestion);
        sortSuggestions();
    }

    private synchronized void sortSuggestions() {
        List<Suggestion> tempList = new ArrayList<>(suggestions);
        Collections.sort(tempList, (s1, s2) -> {
            int distanceCompare = Double.compare(s2.getDistance(), s1.getDistance());
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            int countCompare = Integer.compare(s2.getRepetitionCount(), s1.getRepetitionCount());
            if (countCompare != 0) {
                return countCompare;
            }
            return s1.getScript().toString().compareTo(s2.getScript().toString());
        });
        suggestions.clear();
        suggestions.addAll(tempList);
    }

    public synchronized boolean isEmpty() {
        return suggestions.isEmpty();
    }

    public synchronized Suggestion first() {
        return isEmpty() ? null : suggestions.get(0);
    }

    public synchronized List<Suggestion> toList() {
        return new ArrayList<>(suggestions);
    }

    public synchronized void addAll(SuggestionSet other) {
        for (Suggestion suggestion : other.toList()) {
            add(suggestion);
        }
    }
}