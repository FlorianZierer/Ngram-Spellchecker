import lingologs.Script;
import lingologs.Texture;

public class SpellCheckerUtils {
    public static void printWordInfo(Script originalWord, Prediction prediction) {
        System.out.println("\nWort: " + originalWord);
        System.out.println("Korrigiert zu: " + prediction.getPrediction());
    }

    public static void printSuggestions(String category, Texture<Suggestion> suggestions, boolean directModeEnabled) {
        if (directModeEnabled && !category.equals("Direct")) {
            return;
        }
        System.out.println("Vorschläge (" + category + "):");
        int count = 0;
        for (Suggestion suggestion : suggestions) {
            if (count >= 5) break;
            System.out.println("  - " + suggestion.getScript() + " (Distanz: " + suggestion.getDistance() + ", Frequenz: " + suggestion.getRepetitionCount() + ")");
            count++;
        }
    }

    public static void printInputAndCorrectedWords(Texture<Script> words, Texture<Script> correctedWords) {
        System.out.println("Eingegebene Wörter: " + words);
        System.out.println("Korrigierte Wörter: " + correctedWords);
    }
}