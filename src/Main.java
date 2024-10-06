import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Main {

	public static void consoleListener(SpellChecker spellChecker) {
		Scanner scanner = new Scanner(System.in);
		// Muster für zu löschende Elemente (auskommentiert)
		System.out.println("Geben Sie Wörter ein um sie zu prüfen (oder 'exit' zum Beenden):");
		String[] words;
		String[] correctedWords;
		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}
			words = input.toLowerCase().split(" ");

			System.out.println(Arrays.toString(words));

			correctedWords = new String[words.length];
			for(int i=0 ;i < words.length; i++){
				if(i==0){
					correctedWords[i] = spellChecker.getCorrection(null, words[i], words[i+1],false);
				} else if (i == words.length-1) {
					correctedWords[i] = spellChecker.getCorrection(words[i-1], words[i], null,false);
				} else {
					correctedWords[i] = spellChecker.getCorrection(words[i-1], words[i], words[i+1],false);
				}
			}
			System.out.println(Arrays.toString(correctedWords));
		}

		scanner.close();
	}

	// Quelle: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws IOException {
		Path path = Path.of("./Transcripts/");
		double percent = 0.1; // Bei höheren Prozentzahlen läuft mein Computer out of Memory
		int nGramLength = 3;
		int mircothreads = 50;

		Double acceptanceThreshold = 0.60;
		SpellChecker spellChecker = new SpellChecker(acceptanceThreshold);
		spellChecker.setCorpora(path, percent, nGramLength, mircothreads);

		SpellcheckerEvaluator evaluator = new SpellcheckerEvaluator(spellChecker);
		evaluator.evaluate(false);

		consoleListener(spellChecker);
	}
}