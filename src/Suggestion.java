import lingolava.Mathx;
import lingologs.Script;

class Suggestion {
    // Distanz des Vorschlags
    private double distance;
    // Das zugehörige Skript
    private Script script;
    // Zähler für Wiederholungen
    private int repetitionCount = 0;

    // Konstruktor für einen neuen Vorschlag
    public Suggestion(double distance, Script script) {
        this.distance = distance;
        this.script = script;
    }

    public void merge(Suggestion s) {
        if (s.script.equals(script)) {
            int oldCount = this.repetitionCount;

            if (this.repetitionCount == s.repetitionCount) {
                this.repetitionCount += 1;
            } else {
                this.repetitionCount += s.repetitionCount;
            }
            this.distance = Math.max(s.distance, distance);
        }
    }

    // Erhöht den Wiederholungszähler
    public void incrementRepetitionCount() {
        this.repetitionCount++;
    }

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
}