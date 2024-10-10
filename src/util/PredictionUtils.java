package util;

import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;

import java.util.*;

// Klasse für Vorhersage-Operationen
public class PredictionUtils {

    // Quelle die beim verstehen geholfen hat: https://datascience.stackexchange.com/questions/63325/cosine-similarity-vs-the-levenshtein-distance
    // Ich habe Gpt gefragt, ob eine kombinierte Nutzung Sinn macht: Das kombinierte Verwenden von Levenshtein- und Cosinus-Distanz ist sinnvoll, da Levenshtein Zeichenunterschiede misst und Cosinus-Distanz semantische Ähnlichkeit zwischen Texten erfasst. So wird sowohl die strukturelle als auch die inhaltliche Übereinstimmung bewertet.
    // Berechnet die Distanz zwischen zwei Wörtern


    public static double distance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return 0.0;
        }

        double cosineSimularity = word1.similares(word2, Legacy.Similitude.Cosine);
        double levenshteinDistance = word1.similares(word2, Legacy.Similitude.Levenshtein);
        return (0.2 * cosineSimularity) + (0.8 * levenshteinDistance);
    }

    public static Texture<Prediction> deduplicateAndSortPredictions(Texture<Prediction> predictions) {
        List<Prediction> mutable = predictions.toList();
                mutable.forEach(Prediction::reduceAllLists);
        return new Texture<>(mutable);
    }
}