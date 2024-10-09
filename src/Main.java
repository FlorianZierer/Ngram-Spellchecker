import lingologs.Script;
import lingologs.Texture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {

	public static void consoleListener(SpellChecker spellChecker) throws IOException, ExecutionException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Geben Sie Wörter ein, um sie zu prüfen (oder 'exit' zum Beenden):");

		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}
			Texture<Script> words = new Texture<>(new Script(input.toLowerCase()).split(" "));

			Texture<Prediction> predictions = spellChecker.getPredictions(words, 10, 3, 0.60);
			Texture<Script> correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());

			for (int i = 0; i < words.extent(); i++) {
				Script originalWord = words.at(i);
				Prediction prediction = predictions.at(i);

				System.out.println("\nWort: " + originalWord);
				System.out.println("Korrigiert zu: " + prediction.getPrediction());

				printSuggestions("TriGram", prediction.getSuggestionsTriGram());
				printSuggestions("BiGram", prediction.getSuggestionsBiGram());
				printSuggestions("Direct", prediction.getSuggestionsDirect());
			}

			System.out.println("Eingegebene Wörter: " + words);
			System.out.println("Korrigierte Wörter: " + correctedWords);
		}
		scanner.close();
	}

	private static void printSuggestions(String category, Texture<Suggestion> suggestions) {
		System.out.println("Vorschläge (" + category + "):");
		int count = 0;
		for (Suggestion suggestion : suggestions) {
			if (count >= 5) break;
			System.out.println("  - " + suggestion.getScript() + " (Distanz: " + suggestion.getDistance() + ", Frequenz: " + suggestion.getRepetitionCount() + ")");
			count++;
		}
	}


	// Quelle: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		Path path = Path.of("./Transcripts/");

		double percent = 1; // Bei höheren Prozentzahlen läuft mein Computer out of Memory
		int nGramLength = 3;
		int mircothreads = 10;
		int epochs = 10;

		Double acceptanceThreshold = 0.60;
		SpellChecker spellChecker = new SpellChecker(acceptanceThreshold);
		spellChecker.setCorpora(path, percent, nGramLength, mircothreads, epochs);

		//SpellcheckerEvaluator evaluator = new SpellcheckerEvaluator(spellChecker);
		//evaluator.evaluate(false);

		consoleListener(spellChecker);
	}
}