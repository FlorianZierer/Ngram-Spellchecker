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

public class SpellcheckerEvaluator {
    private static final String EVALUATION_DATASET_PATH = ".\\Transcripts\\Evaluation\\evaluation_dataset.json";
    private SpellChecker spellChecker;

    public SpellcheckerEvaluator(SpellChecker spellChecker) {
        this.spellChecker = spellChecker;
    }

    public double evaluate(boolean directMode) throws IOException, ExecutionException, InterruptedException {
        List<Map<String, String>> dataset = loadDataset(Path.of(EVALUATION_DATASET_PATH));
        int totalSentences = dataset.size();
        int correctlyCorrected = 0;

        System.out.println(Constants.ANSI_CYAN + "Dataset importiert. Gesamtzahl der Sätze: " + totalSentences + Constants.ANSI_RESET);
        System.out.println(Constants.ANSI_CYAN + "Modus: " + (directMode ? "Nur direkte Vorschläge" : "Alle Vorschläge") + Constants.ANSI_RESET);
        System.out.println();

        for (Map<String, String> entry : dataset) {
            String correct = entry.get("correct");
            String incorrect = entry.get("incorrect");

            System.out.println(Constants.ANSI_BLUE + "Satz: " + incorrect + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);

            Texture<Script> words = new Texture<>(new Script(incorrect.toLowerCase()).split(" "));
            Texture<Prediction> predictions = spellChecker.getPredictions(words, 10, 3, directMode);
            Texture<Script> correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());

            String correctedSentence = String.join(" ", correctedWords.map(Script::toString).toList());
            boolean isCorrect = correctedSentence.equals(correct);

            for (int i = 0; i < words.extent(); i++) {
                Script originalWord = words.at(i);
                Prediction prediction = predictions.at(i);

                SpellCheckerUtils.printWordInfo(originalWord, prediction);

                if (!originalWord.equals(prediction.getPrediction())) {
                    System.out.printf(Constants.ANSI_PURPLE + " %s → %s%n" + Constants.ANSI_RESET, originalWord, prediction.getPrediction());
                } else {
                    System.out.println(originalWord + " (Keine Änderung)");
                }

                if (!directMode) {
                    SpellCheckerUtils.printSuggestions("TriGram", prediction.getSuggestionsTriGram(), directMode);
                    SpellCheckerUtils.printSuggestions("BiGram", prediction.getSuggestionsBiGram(), directMode);
                }
                SpellCheckerUtils.printSuggestions("Direct", prediction.getSuggestionsDirect(), directMode);
            }

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

        double accuracy = (double) correctlyCorrected / totalSentences;
        System.out.printf(Constants.ANSI_GREEN + "Genauigkeit: %.2f%%%n" + Constants.ANSI_RESET, accuracy * 100);
        return accuracy;
    }

    public static List<Map<String, String>> loadDataset(Path EVALUATION_DATASET_PATH) throws IOException {
        String evalData = Files.readString(EVALUATION_DATASET_PATH);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(evalData);

        return readNgram.asList(outerNote ->
                outerNote.asMap(Nexus.DataNote::asString, Nexus.DataNote::asString)
        );
    }
}