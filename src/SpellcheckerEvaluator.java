import lingolava.Nexus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpellcheckerEvaluator {
    // Pfad zum Evaluierungsdatensatz
    private static final String EVALUATION_DATASET_PATH = ".\\Transcripts\\Evaluation\\evaluation_dataset.json";
    private SpellChecker spellChecker;

    // Konstruktor für den Evaluator
    public SpellcheckerEvaluator(SpellChecker spellChecker) {
        this.spellChecker = spellChecker;
    }

    // Hauptmethode zur Evaluierung des Spellcheckers
    public double evaluate(boolean directOnly) throws IOException {
        List<Map<String, String>> dataset = loadDataset(Path.of(EVALUATION_DATASET_PATH));
        int totalSentences = dataset.size();
        int correctlyCorrected = 0;

        // Ausgabe der Evaluierungsdetails
        System.out.println(Constants.ANSI_CYAN + "Dataset importiert. Gesamtzahl der Sätze: " + totalSentences + Constants.ANSI_RESET);
        System.out.println(Constants.ANSI_CYAN + "Evaluierungsmodus: " + (directOnly ? "Nur direkte Distanz" : "Alle Distanzen") + Constants.ANSI_RESET);
        System.out.println();

        // Schleife durch alle Sätze im Datensatz
        for (Map<String, String> entry : dataset) {
            String correct = entry.get("correct");
            String incorrect = entry.get("incorrect");

            String[] words = incorrect.toLowerCase().split(" ");
            String[] correctedWords = new String[words.length];

            // Ausgabe des zu korrigierenden Satzes
            System.out.println(Constants.ANSI_BLUE + "Satz: " + incorrect + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);

            // Korrektur jedes Wortes im Satz
            for (int i = 0; i < words.length; i++) {
                String prevWord = (i > 0) ? words[i - 1] : null;
                String nextWord = (i < words.length - 1) ? words[i + 1] : null;

                System.out.println(Constants.ANSI_BLUE + "Überprüfe: " + words[i] + Constants.ANSI_RESET);
                System.out.println("Direkte Vorschläge:");

                correctedWords[i] = spellChecker.getCorrection(prevWord, words[i], nextWord, directOnly);

                // Ausgabe der Korrektur
                if (!words[i].equals(correctedWords[i])) {
                    System.out.printf(Constants.ANSI_PURPLE + " %s → %s%n" + Constants.ANSI_RESET, words[i], correctedWords[i]);
                } else {
                    System.out.println(words[i] + " (Keine Änderung)");
                }
            }

            // Zusammensetzen des korrigierten Satzes
            String correctedSentence = String.join(" ", correctedWords);
            boolean isCorrect = correctedSentence.equals(correct);

            // Ausgabe der Ergebnisse
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "Falsch:     " + incorrect);
            System.out.println(Constants.ANSI_GREEN + "Korrekt:    " + correct);
            System.out.println(Constants.ANSI_CYAN + "Vorhergesagt: " + correctedSentence);
            System.out.println(Constants.ANSI_YELLOW + "Ergebnis:    " + (isCorrect ? Constants.ANSI_GREEN + "RICHTIG" : Constants.ANSI_RED + "FALSCH") + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println();

            if (isCorrect) {
                correctlyCorrected++;
            }
        }

        // Berechnung und Ausgabe der Genauigkeit
        double accuracy = (double) correctlyCorrected / totalSentences;
        System.out.printf(Constants.ANSI_GREEN + "Genauigkeit: %.2f%%%n" + Constants.ANSI_RESET, accuracy * 100);
        return accuracy;
    }

    // Methode zum Laden des Evaluierungsdatensatzes
    public static List<Map<String, String>> loadDataset(Path EVALUATION_DATASET_PATH) throws IOException {
        String evalData = Files.readString(EVALUATION_DATASET_PATH);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(evalData);

        return readNgram.asList(outerNote ->
                outerNote.asMap(Nexus.DataNote::asString, Nexus.DataNote::asString
                )
        );
    }
}