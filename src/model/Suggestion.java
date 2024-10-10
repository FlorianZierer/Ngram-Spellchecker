package model;

import lingologs.Script;

// Klasse zur Repräsentation eines Vorschlags (Suggestion) für eine Vorhersage
public class Suggestion {
    // Distanz des Vorschlags
    private double distance;
    // Das zugehörige Skript
    private Script script;
    // Zähler für Wiederholungen
    private int repetitionCount = 1;

    // Konstruktor für einen neuen Vorschlag
    public Suggestion(double distance, Script script) {
        this.distance = distance;
        this.script = script;
    }

    // Methode zum Zusammenführen von zwei Suggestions
    public Suggestion merge(Suggestion other) {
        if (!this.script.equals(other.script)) {
            throw new IllegalArgumentException("Cannot merge Suggestions with different scripts");
        }
        this.repetitionCount += other.repetitionCount;
        return this;
    }

    // Erhöht den Wiederholungszähler
    public void incrementRepetitionCount() {
        this.repetitionCount++;
    }

    // Getter- und Setter-Methoden

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public int getRepetitionCount() {
        return repetitionCount;
    }

    public void setRepetitionCount(int repetitionCount) {
        this.repetitionCount = repetitionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suggestion that = (Suggestion) o;
        return script.equals(that.script);
    }

    @Override
    public int hashCode() {
        return script.hashCode();
    }
}