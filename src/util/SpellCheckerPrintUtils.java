package util;

import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import model.Suggestion;
import model.SuggestionSet;

// Klasse für Rechtschreibprüfungs-Operationen
public class SpellCheckerPrintUtils {
    // Methode zum Ausgeben von Wortinformationen
    public static void printWordInfo(Script originalWord, Prediction prediction) {
        System.out.println("\nWort: " + originalWord);
        System.out.println("Korrigiert zu: " + prediction.getPrediction());
    }

    // Methode zum Ausgeben von Vorschlägen
    public static void printSuggestions(String category, SuggestionSet suggestions, boolean directModeEnabled) {
        if (directModeEnabled && !category.equals("Direct")) {
            return;
        }
        System.out.println("Vorschläge (" + category + "):");
        int count = 0;
        for (Suggestion suggestion : suggestions.toList()) {
            if (count >= 5) break;
            System.out.println("  - " + suggestion.getScript() + " (Distanz: " + suggestion.getDistance() + ", Frequenz: " + suggestion.getRepetitionCount() + ")");
            count++;
        }
    }

    // Methode zum Ausgeben der eingegebenen und korrigierten Wörter
    public static void printInputAndCorrectedWords(Texture<Script> words, Texture<Script> correctedWords) {
        System.out.println("Eingegebene Wörter: " + words);
        System.out.println("Korrigierte Wörter: " + correctedWords);
    }
}