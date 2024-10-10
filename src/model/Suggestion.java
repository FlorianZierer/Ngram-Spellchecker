package model;

import lingologs.Script;

public class Suggestion {
    private final double distance;
    private final Script script;
    private final int repetitionCount;

    public Suggestion(double distance, Script script) {
        this(distance, script, 1);
    }

    public Suggestion(double distance, Script script, int repetitionCount) {
        this.distance = distance;
        this.script = script;
        this.repetitionCount = repetitionCount;
    }

    public double getDistance() {
        return distance;
    }

    public Script getScript() {
        return script;
    }

    public int getRepetitionCount() {
        return repetitionCount;
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