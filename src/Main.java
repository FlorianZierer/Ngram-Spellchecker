import lingolava.Legacy;
import lingolava.Nexus.*;
import lingolava.Tuple;
import lingologs.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lingolava.Tuple.*;

public class Main {

	public static void consoleListener() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Geben Sie drei Wörter ein (oder 'exit' zum Beenden):");

		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("exit")) {
				System.out.println("Programm beendet.");
				break;
			}

			String[] words = input.toLowerCase().split(" ");
			if (words.length != 3) {
				System.out.println("Bitte geben Sie genau drei Wörter ein.");
				continue;
			}

			String word1 = words[0];
			String word2 = words[1];
			String word3 = words[2];

		}

		scanner.close();
	}

    // Source: https://wortschatz.uni-leipzig.de/en/download/English
    public static void main(String[] args) {
        String path = "./Transcripts/";
        double percent = 0.10;
        int nGramLength = 3;
		Double acceptanceThreshold = 0.70;
		SpellChecker spellChecker = new SpellChecker(acceptanceThreshold);

		spellChecker.setCorpora(path, percent, nGramLength);

        consoleListener();
    }
}

