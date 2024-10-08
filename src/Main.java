import lingologs.Script;
import lingologs.Texture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {

	public static void consoleListener(SpellChecker spellChecker) throws IOException, ExecutionException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		// Muster für zu löschende Elemente (auskommentiert)
		System.out.println("Geben Sie Wörter ein um sie zu prüfen (oder 'exit' zum Beenden):");
		Texture<Script> words;
		Texture<Script> correctedWords;
		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}
			words = new Texture(new Script(input).toLower().split(" "));

			Texture<Prediction> predictions = spellChecker.getPredictions(words,10,3,0.60);
			correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());
			System.out.println(words);
			System.out.println(correctedWords);
		}

		scanner.close();
	}

	// Quelle: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws IOException {
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