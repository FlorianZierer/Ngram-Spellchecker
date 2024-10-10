package util;

import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PredictionUtils {
    private static final Map<Pair<Script, Script>, Double> distanceCache = new ConcurrentHashMap<>();

    public static double distance(Script word1, Script word2) {
        Pair<Script, Script> key = new Pair<>(word1, word2);
        return distanceCache.computeIfAbsent(key, k -> computeDistance(word1, word2));
    }

    private static double computeDistance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return 0.0;
        }

        double cosineSimularity = word1.similares(word2, Legacy.Similitude.Cosine);
        double levenshteinDistance = word1.similares(word2, Legacy.Similitude.Levenshtein);
        return (0.2 * cosineSimularity) + (0.8 * levenshteinDistance);
    }

    public static Texture<Prediction> deduplicateAndSortPredictions(Texture<Prediction> predictions) {
        List<Prediction> mutable = predictions.toList();
        mutable.forEach(Prediction::getPrediction); // This will compute and cache predictions
        return new Texture<>(mutable);
    }

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