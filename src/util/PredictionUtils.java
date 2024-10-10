package util;

import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PredictionUtils {
    // Cache für die Distanzberechnungen zwischen Skript-Paaren
    // Wird verwendet, um wiederholte Berechnungen zu vermeiden und die Leistung zu verbessern
    private static final Map<Pair<Script, Script>, Double> distanceCache = new ConcurrentHashMap<>();

    // Berechnet die Distanz zwischen zwei Skripten
    // Nutzt den Cache, um bereits berechnete Distanzen wiederzuverwenden
    public static double distance(Script word1, Script word2) {
        Pair<Script, Script> key = new Pair<>(word1, word2);
        return distanceCache.computeIfAbsent(key, k -> computeDistance(word1, word2));
    }

    // Tatsächliche Berechnung der Distanz zwischen zwei Skripten
    // Kombiniert Cosinus-Ähnlichkeit und Levenshtein-Distanz
    private static double computeDistance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return 0.0;
        }
        double cosineSimularity = word1.similares(word2, Legacy.Similitude.Cosine);
        double levenshteinDistance = word1.similares(word2, Legacy.Similitude.Levenshtein);
        return (0.2 * cosineSimularity) + (0.8 * levenshteinDistance);
    }

    // Dedupliziert und sortiert Vorhersagen
    // Hier wird das Caching der Vorhersagen explizit genutzt
    public static Texture<Prediction> deduplicateAndSortPredictions(Texture<Prediction> predictions) {
        List<Prediction> mutable = predictions.toList();
        // Berechnet und cached die Vorhersagen für jedes Element
        // Dies ist wichtig, da die Berechnung von Vorhersagen möglicherweise aufwändig ist
        // Durch das Caching werden wiederholte Berechnungen vermieden, was die Leistung verbessert
        mutable.forEach(Prediction::getPrediction);
        return new Texture<>(mutable);
    }

    // Löscht den Cache
    // Nützlich, um Speicher freizugeben oder den Cache zurückzusetzen
    public static void clearCache() {
        distanceCache.clear();
    }

    // Interne Klasse zur Repräsentation von Paaren
    // Wird für den Distanz-Cache verwendet
    private static class Pair<K, V> {
        private final K first;
        private final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(first, pair.first) &&
                    Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}