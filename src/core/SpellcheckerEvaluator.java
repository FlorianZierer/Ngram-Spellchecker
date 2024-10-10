package core;

import constants.Constants;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.PredictionUtils;
import util.SpellCheckerPrintUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

// Klasse zur Evaluierung des Rechtschreibprüfers
public class SpellcheckerEvaluator {
    private static final String EVALUATION_DATASET_PATH = ".\\Transcripts\\Evaluation\\evaluation_dataset.json";
    private static final int BATCH_SIZE = 10; // Größe der zu verarbeitenden Batches
    private SpellChecker spellChecker;

    // Konstruktor für den SpellcheckerEvaluator
    public SpellcheckerEvaluator(SpellChecker spellChecker) {
        this.spellChecker = spellChecker;
    }

    // Methode zur Evaluierung des Rechtschreibprüfers
    public double evaluate(boolean directMode) throws IOException, ExecutionException, InterruptedException {
        List<Map<String, String>> dataset = loadDataset(Path.of(EVALUATION_DATASET_PATH));
        int totalSentences = dataset.size();
        int correctlyCorrected = 0;

        System.out.println(Constants.ANSI_CYAN + "Dataset importiert. Gesamtzahl der Sätze: " + totalSentences + Constants.ANSI_RESET);
        System.out.println(Constants.ANSI_CYAN + "Modus: " + (directMode ? "Nur direkte Vorschläge" : "Alle Vorschläge") + Constants.ANSI_RESET);
        System.out.println();

        for (int batchStart = 0; batchStart < totalSentences; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, totalSentences);
            List<Map<String, String>> batchDataset = dataset.subList(batchStart, batchEnd);

            correctlyCorrected += processBatch(batchDataset, directMode, batchStart);
        }

        double accuracy = (double) correctlyCorrected / totalSentences;
        System.out.printf(Constants.ANSI_GREEN + "Genauigkeit: %.2f%%%n" + Constants.ANSI_RESET, accuracy * 100);
        return accuracy;
    }

    // Methode zur Verarbeitung eines Batches von Sätzen
    private int processBatch(List<Map<String, String>> batchDataset, boolean directMode, int batchStart) throws ExecutionException, InterruptedException, IOException {
        int correctlyCorrected = 0;
        List<Script> allPaddedWords = new ArrayList<>();

        for (Map<String, String> entry : batchDataset) {
            String incorrect = entry.get("incorrect");
            List<Script> paddedSentence = padSentence(incorrect);
            allPaddedWords.addAll(paddedSentence);
        }

        Texture<Script> allWords = new Texture<>(allPaddedWords);
        Texture<Prediction> allPredictions = spellChecker.getPredictions(allWords, directMode);

        int wordIndex = 0;
        for (Map<String, String> entry : batchDataset) {
            String correct = entry.get("correct");
            String incorrect = entry.get("incorrect");

            System.out.println();
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println();
            System.out.println(Constants.ANSI_BLUE + "Satz " + (batchStart + wordIndex / 3 + 1) + ": " + incorrect + Constants.ANSI_RESET);

            List<Script> paddedSentence = padSentence(incorrect);
            List<Prediction> sentencePredictions = new ArrayList<>();
            List<Script> correctedWords = new ArrayList<>();

            for (int i = 0; i < paddedSentence.size(); i++) {
                Prediction prediction = allPredictions.at(wordIndex + i);
                sentencePredictions.add(prediction);
                correctedWords.add(prediction.getPrediction());
            }

            String correctedSentence = String.join(" ", correctedWords.subList(1, correctedWords.size() - 1)
                    .stream()
                    .map(Script::toString)
                    .toList());
            boolean isCorrect = correctedSentence.equals(correct);

            for (int i = 1; i < paddedSentence.size() - 1; i++) {
                Script originalWord = paddedSentence.get(i);
                Prediction prediction = sentencePredictions.get(i);

                SpellCheckerPrintUtils.printWordInfo(originalWord, prediction);

                if (!originalWord.equals(prediction.getPrediction())) {
                    System.out.printf(Constants.ANSI_PURPLE + " %s → %s%n" + Constants.ANSI_RESET, originalWord, prediction.getPrediction());
                } else {
                    System.out.println(originalWord + " (Keine Änderung)");
                }

                if (!directMode) {
                    SpellCheckerPrintUtils.printSuggestions("TriGram", prediction.getSuggestionsTriGram(), directMode);
                    SpellCheckerPrintUtils.printSuggestions("BiGram", prediction.getSuggestionsBiGram(), directMode);
                }
                SpellCheckerPrintUtils.printSuggestions("Direct", prediction.getSuggestionsDirect(), directMode);
            }

            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "Falsch:     " + incorrect);
            System.out.println(Constants.ANSI_GREEN + "Korrekt:    " + correct);
            System.out.println(Constants.ANSI_CYAN + "Vorhergesagt: " + correctedSentence);
            System.out.println(Constants.ANSI_YELLOW + "Ergebnis:    " + (isCorrect ? Constants.ANSI_GREEN + "RICHTIG" : Constants.ANSI_RED + "FALSCH") + Constants.ANSI_RESET);
            System.out.println(Constants.ANSI_YELLOW + "----------------------------------------------------------------------" + Constants.ANSI_RESET);
            System.out.println();

            SpellCheckerPrintUtils.printInputAndCorrectedWords(new Texture<>(paddedSentence.subList(1, paddedSentence.size() - 1)),
                    new Texture<>(correctedWords.subList(1, correctedWords.size() - 1)));

            if (isCorrect) {
                correctlyCorrected++;
            }

            wordIndex += paddedSentence.size();
        }

        PredictionUtils.clearCache();
        for (Prediction prediction : allPredictions.toList()) {
            prediction.clear();
        }

        return correctlyCorrected;
    }

    // Methode zum Hinzufügen von Polsterung zu einem Satz um Anfang und Ende zu kennzeichnen
    private List<Script> padSentence(String sentence) {
        List<Script> words = new ArrayList<>();
        words.add(new Script("")); // Polsterung am Anfang hinzufügen
        for (String word : sentence.toLowerCase().split(" ")) {
            words.add(new Script(word));
        }
        words.add(new Script("")); // Polsterung am Ende hinzufügen
        return words;
    }

    // Methode zum Laden des Evaluierungsdatensatzes
    public static List<Map<String, String>> loadDataset(Path EVALUATION_DATASET_PATH) throws IOException {
        String evalData = Files.readString(EVALUATION_DATASET_PATH);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(evalData);

        return readNgram.asList(outerNote ->
                outerNote.asMap(Nexus.DataNote::asString, Nexus.DataNote::asString)
        );
    }
}