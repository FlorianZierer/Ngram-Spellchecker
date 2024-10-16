package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Diese Klasse verwendet eine spezialisierte Datenstruktur anstelle einer normalen Liste
// um effiziente und threadsichere Operationen auf Vorschlägen zu ermöglichen
public class SuggestionSet {
    // CopyOnWriteArrayList wird verwendet, um Threadsicherheit zu gewährleisten
    private final List<Suggestion> suggestions;

    public SuggestionSet() {
        this.suggestions = new CopyOnWriteArrayList<>();
    }

    // Synchronisierte Methode zum Hinzufügen oder Aktualisieren von Vorschlägen
    // Dies verhindert Konflikte bei gleichzeitigen Zugriffen
    public synchronized void add(Suggestion suggestion) {
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion existing = suggestions.get(i);
            if (existing.getScript().equals(suggestion.getScript())) {
                // Aktualisiert einen vorhandenen Vorschlag, anstatt einen neuen hinzuzufügen
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

    // Sortiert die Vorschläge nach benutzerdefinierten Kriterien
    // Dies wäre mit einer normalen Liste schwieriger zu implementieren

// Quellen: https://docs.oracle.com/cd/E19455-01/806-5257/6je9h0346/index.html#:~:text=The%20threads%20in%20an%20application,something%20that%20manipulates%20an%20object.
    //https://www.baeldung.com/java-synchronized
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

    // Weitere synchronisierte Methoden für threadsichere Operationen

    public synchronized void clear() {
        suggestions.clear();
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