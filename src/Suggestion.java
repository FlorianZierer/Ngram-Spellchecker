import lingologs.Script;

class Suggestion {
    // Bewertung des Vorschlags
    double score;
    // Das zugehörige Skript
    Script script;
    // Zähler für Wiederholungen
    int repetitionCount;

    // Konstruktor für einen neuen Vorschlag
    public Suggestion(double score, Script script) {
        this.score = score;
        this.script = script;
        this.repetitionCount = 1;
    }

    // Erhöht den Wiederholungszähler
    public void incrementRepetitionCount() {
        this.repetitionCount++;
    }
}