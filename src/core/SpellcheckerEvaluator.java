package core;

import constants.Constants;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.SpellCheckerUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

// Diese Klasse evaluiert die Leistung des Rechtschreibprüfers
public class SpellcheckerEvaluator {
    private static final String EVALUATION_DATASET_PATH = ".\\Transcripts\\Evaluation\\evaluation_dataset.json";
    private SpellChecker spellChecker;

    // Konstruktor, der eine Instanz des zu evaluierenden SpellChecker erhält
    public SpellcheckerEvaluator(SpellChecker spellChecker) {
        this.spellChecker = spellChecker;
    }

    // Hauptmethode zur Durchführung der Evaluation
    public double evaluate(boolean directMode) throws IOException, ExecutionException, InterruptedException {
        List<Map<String, String>> dataset = loadDataset(Path.of(EVALUATION_DATASET_PATH));
        int totalSentences = dataset.size();
        int correctlyCorrected = 0;

        // Gibt Informationen über den Datensatz und den Evaluierungsmodus aus
        System.out.println(Constants.ANSI_CYAN + "Dataset importiert. Gesamtzahl der Sätze: " + totalSentences + Constants.ANSI_RESET);
        System.out.println(Constants.ANSI_CYAN + "Modus: " + (directMode ? "Nur direkte Vorschläge" : "Alle Vorschläge") + Constants.ANSI_RESET);
        System.out.println();

        // Iteriert über alle Sätze im Datensatz
        for (Map<String, String> entry : dataset) {
            String correct = entry.get("correct");
            String incorrect = entry.get("incorrect");

            // Gibt den zu korrigierenden Satz aus
            System.out.println(Constants.ANSI_BLUE + "Satz: " + incorrect + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);

            // Generiert Vorhersagen für den fehlerhaften Satz
            Texture<Script> words = new Texture<>(new Script(incorrect.toLowerCase()).split(" "));
            Texture<Prediction> predictions = spellChecker.getPredictions(words, directMode);
            Texture<Script> correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());

            // Erstellt den korrigierten Satz und überprüft, ob er korrekt ist
            String correctedSentence = String.join(" ", correctedWords.map(Script::toString).toList());
            boolean isCorrect = correctedSentence.equals(correct);

            // Gibt Details für jedes Wort und seine Vorhersagen aus
            for (int i = 0; i < words.extent(); i++) {
                Script originalWord = words.at(i);
                Prediction prediction = predictions.at(i);

                SpellCheckerUtils.printWordInfo(originalWord, prediction);

                if (!originalWord.equals(prediction.getPrediction())) {
                    System.out.printf(Constants.ANSI_PURPLE + " %s → %s%n" + Constants.ANSI_RESET, originalWord, prediction.getPrediction());
                } else {
                    System.out.println(originalWord + " (Keine Änderung)");
                }

                // Gibt zusätzliche Vorschläge aus, wenn nicht im direkten Modus
                if (!directMode) {
                    SpellCheckerUtils.printSuggestions("TriGram", prediction.getSuggestionsTriGram(), directMode);
                    SpellCheckerUtils.printSuggestions("BiGram", prediction.getSuggestionsBiGram(), directMode);
                }
                SpellCheckerUtils.printSuggestions("Direct", prediction.getSuggestionsDirect(), directMode);
            }

            // Gibt das Ergebnis für den aktuellen Satz aus
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "Falsch:     " + incorrect);
            System.out.println(Constants.ANSI_GREEN + "Korrekt:    " + correct);
            System.out.println(Constants.ANSI_CYAN + "Vorhergesagt: " + correctedSentence);
            System.out.println(Constants.ANSI_YELLOW + "Ergebnis:    " + (isCorrect ? Constants.ANSI_GREEN + "RICHTIG" : Constants.ANSI_RED + "FALSCH") + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println();

            SpellCheckerUtils.printInputAndCorrectedWords(words, correctedWords);

            if (isCorrect) {
                correctlyCorrected++;
            }
        }

        // Berechnet und gibt die Gesamtgenauigkeit aus
        double accuracy = (double) correctlyCorrected / totalSentences;
        System.out.printf(Constants.ANSI_GREEN + "Genauigkeit: %.2f%%%n" + Constants.ANSI_RESET, accuracy * 100);
        return accuracy;
    }

    // Methode zum Laden des Evaluierungsdatensatzes aus einer JSON-Datei
    public static List<Map<String, String>> loadDataset(Path EVALUATION_DATASET_PATH) throws IOException {
        String evalData = Files.readString(EVALUATION_DATASET_PATH);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(evalData);

        return readNgram.asList(outerNote ->
                outerNote.asMap(Nexus.DataNote::asString, Nexus.DataNote::asString)
        );
    }
}