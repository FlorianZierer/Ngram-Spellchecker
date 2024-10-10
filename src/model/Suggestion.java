package model;

import lingologs.Script;

public class Suggestion {
    // Eigenschaften der Klasse
    private final double distance;
    private final Script script;
    private final int repetitionCount;

    // Konstruktor mit Standardwiederholungszähler
    public Suggestion(double distance, Script script) {
        this(distance, script, 1);
    }

    // Konstruktor mit benutzerdefiniertem Wiederholungszähler
    public Suggestion(double distance, Script script, int repetitionCount) {
        this.distance = distance;
        this.script = script;
        this.repetitionCount = repetitionCount;
    }

    // Getter-Methoden
    public double getDistance() {
        return distance;
    }

    public Script getScript() {
        return script;
    }

    public int getRepetitionCount() {
        return repetitionCount;
    }

    // Überschreibt die equals-Methode für Vergleiche
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suggestion that = (Suggestion) o;
        return script.equals(that.script);
    }

    // Überschreibt die hashCode-Methode für effiziente Speicherung in Hash-basierten Sammlungen
    @Override
    public int hashCode() {
        return script.hashCode();
    }
}