import constants.Constants;
import core.SpellChecker;
import core.SpellcheckerEvaluator;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.SpellCheckerUtils;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class Main {

	private static final Pattern VALID_INPUT_PATTERN = Pattern.compile("[a-z'&\\- ]+$");

	// Methode für die Konsoleneingabe und Rechtschreibprüfung
	public static void consoleListener(SpellChecker spellChecker, boolean directmode) throws IOException, ExecutionException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println(Constants.ANSI_PURPLE + "Geben Sie Wörter ein, um sie zu prüfen (oder 'exit' zum Beenden):" + Constants.ANSI_PURPLE);

		while (true) {
			String input = scanner.nextLine().toLowerCase(); // Convert input to lowercase
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}

			// Überprüfen Sie die Eingabe auf gültige Zeichen
			if (!VALID_INPUT_PATTERN.matcher(input).matches()) {
				System.out.println(Constants.ANSI_RED + "Ungültige Eingabe. Bitte verwenden Sie nur Kleinbuchstaben, Apostroph ('), Kaufmanns-Und (&), Bindestrich (-) und Leerzeichen."+ Constants.ANSI_RED);
				continue;  // Springe zurück zum Anfang der Schleife für neue Eingabe
			}

			// Eingabe in Wörter aufteilen
			Texture<Script> words = new Texture<>(new Script(input).split(" "));

			// Vorhersagen für die Wörter erhalten
			Texture<Prediction> predictions = spellChecker.getPredictions(words, 10, 3, directmode);
			Texture<Script> correctedWords = new Texture<>(predictions.map(Prediction::getPrediction).toList());

			// Ergebnisse für jedes Wort ausgeben
			for (int i = 0; i < words.extent(); i++) {
				Script originalWord = words.at(i);
				Prediction prediction = predictions.at(i);

				SpellCheckerUtils.printWordInfo(originalWord, prediction);

				// Vorschläge basierend auf verschiedenen Methoden ausgeben
				SpellCheckerUtils.printSuggestions("TriGram", prediction.getSuggestionsTriGram(), directmode);
				SpellCheckerUtils.printSuggestions("BiGram", prediction.getSuggestionsBiGram(), directmode);
				SpellCheckerUtils.printSuggestions("Direct", prediction.getSuggestionsDirect(), directmode);
			}

			// Ursprüngliche und korrigierte Wörter ausgeben
			SpellCheckerUtils.printInputAndCorrectedWords(words, correctedWords);
		}
		scanner.close();
	}


	// Quelle: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		Path path = Path.of("./Transcripts/");

		// Parameter für den SpellChecker festlegen
		double percent = 1;
		int nGramLength = 3;
		int mircothreads = 500;
		int epochs = 5;

		Double acceptanceThreshold = 0.60;
		SpellChecker spellChecker = new SpellChecker(acceptanceThreshold);
		spellChecker.setCorpora(path, percent, nGramLength, mircothreads, epochs);

		//SpellChecker evaluieren
		//SpellcheckerEvaluator evaluator = new SpellcheckerEvaluator(spellChecker);
		//evaluator.evaluate(false);

		consoleListener(spellChecker,false);
	}
}