import core.SpellChecker;
import core.SpellcheckerEvaluator;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.SpellCheckerUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {

	public static void consoleListener(SpellChecker spellChecker, boolean directmode) throws IOException, ExecutionException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Geben Sie Wörter ein, um sie zu prüfen (oder 'exit' zum Beenden):");

		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}
			Texture<Script> words = new Texture<>(new Script(input.toLowerCase()).split(" "));

			Texture<Prediction> predictions = spellChecker.getPredictions(words, 10, 3,directmode);
			Texture<Script> correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());

			for (int i = 0; i < words.extent(); i++) {
				Script originalWord = words.at(i);
				Prediction prediction = predictions.at(i);

				SpellCheckerUtils.printWordInfo(originalWord, prediction);

				SpellCheckerUtils.printSuggestions("TriGram", prediction.getSuggestionsTriGram(),directmode);
				SpellCheckerUtils.printSuggestions("BiGram", prediction.getSuggestionsBiGram(),directmode);
				SpellCheckerUtils.printSuggestions("Direct", prediction.getSuggestionsDirect(),directmode);
			}

			SpellCheckerUtils.printInputAndCorrectedWords(words, correctedWords);
		}
		scanner.close();
	}


	// Quelle: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		Path path = Path.of("./Transcripts/");

		double percent = 1;
		int nGramLength = 3;
		int mircothreads = 10;
		int epochs = 10;

		Double acceptanceThreshold = 0.60;
		SpellChecker spellChecker = new SpellChecker(acceptanceThreshold);
		spellChecker.setCorpora(path, percent, nGramLength, mircothreads, epochs);

		SpellcheckerEvaluator evaluator = new SpellcheckerEvaluator(spellChecker);
		evaluator.evaluate(false);

		//consoleListener(spellChecker,false);
	}
}