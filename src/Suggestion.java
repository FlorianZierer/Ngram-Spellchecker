import lingologs.Script;

class Suggestion {
    // Distanz des Vorschlags
    private double distance;
    // Das zugehörige Skript
    private Script script;
    // Zähler für Wiederholungen
    private int repetitionCount;

    // Konstruktor für einen neuen Vorschlag
    public Suggestion(double distance, Script script) {
        this.distance = distance;
        this.script = script;
    }

    // Erhöht den Wiederholungszähler
    public void incrementRepetitionCount() {
        this.repetitionCount++;
    }

    public double getScore() {
        return distance;
    }

    public void setScore(double distance) {
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
}