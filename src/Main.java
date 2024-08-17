import lingolava.Legacy;
import lingolava.Nexus.*;
import lingologs.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lingolava.Tuple.*;

public class Main {


    public static Script processText(Path path, double percent) {
        try {
            List<String> L = Files.readAllLines(path);
            L.removeIf(String::isEmpty);
            List<String> Lshort = shortenList(L, percent);
            String U = String.join(" ", Lshort);
            return new Script(U);
        } catch (Exception E) {
            throw new RuntimeException(E);
        }
    }

    private static List<String> shortenList(List<String> L, double percent) {
        int endIndex = (int) (L.size() * (percent / 100.0));
        return new ArrayList<>(L.subList(0, Math.min(endIndex, L.size())));
    }
/*
	public static List<List<Script>> convertListIntoSmaller(List<Script> words){
		return IntStream.rangeClosed(0, words.size() - 3)
				.mapToObj(i -> words.subList(i, i + 3))
				.collect(Collectors.toList()); // TODO: get all
	}
	*/

    static public Double levenshtein(Script word1, Script word2) {
        return word1.similares(word2, Legacy.Similitude.Levenshtein);
    }


    private static Texture<Script> readAndFilterTxt(String path, String filename, double percent) {
        Script S = processText(Path.of(path + filename + ".txt"), percent);
        String superScripts = "[⁰¹²³⁴⁵⁶⁷⁸⁹]";
        Pattern toDelete = Pattern.compile("(\\d+\\t)|•|\"" + superScripts); // TODO: to remove », http://cdn. , " , -- ,  ¹, «  / Zitate raus und variation
        Script clearedText = S.replace(toDelete, Script.NIX).toLower();
        return new Texture<>(clearedText.split("\\s+"));
    }

    private static Texture<Texture<Script>> getNgrams(String path, String filename, double percent, Integer nGrammLength) {
        Path jsonFilePath = Path.of(path + "Json/" + filename + ".json");
        Texture<Texture<Script>> wordsGrammyfied;
        try {
            if (Files.exists(jsonFilePath)) {
                String nGramJson = Files.readString(jsonFilePath);
                DataNote readNgram = DataNote.byJSON(nGramJson);
                wordsGrammyfied = new Texture<>(
                        readNgram.asList(d ->
                                new Texture<>(d.asList(inner ->
                                        new Script(inner.asString())
                                ))
                        )
                );


            } else {
                JSONProcessor JP = new JSONProcessor();
                Texture<Script> words = readAndFilterTxt(path, filename, percent);
                wordsGrammyfied = new Texture<>(words.grammy(nGrammLength));
                List<List<Script>> convertedList = wordsGrammyfied
                        .stream()
                        .map(texture -> new ArrayList<>(texture.stream().toList()))
                        .collect(Collectors.toList());

                DataNote json = DataNote.by(convertedList);
                Files.createFile(jsonFilePath);
                String jsonString = JP.present(json);
                Files.writeString(jsonFilePath, jsonString);
            }

            return wordsGrammyfied;
        } catch (Exception E) {
            throw new RuntimeException(E);
        }
    }

	public static void consoleListener(Texture<Texture<Script>> ngrams) {
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

			Set<Couple<Double, Script>> suggestionsTriGram = new HashSet<>();
			Set<Couple<Double, Script>> suggestionsBiGram= new HashSet<>();
			Set<Couple<Double, Script>> suggestionsDirect = new HashSet<>();

			double highestSimilarityTriGram = 0.0;
			double highestSimilarityBiGram = 0.0;
			double highestSimilarityDirect = 0.0;// Looking for the smallest distance
			// Ngram Search
			for (Texture<Script> ngram : ngrams) {
				double distance1 = levenshtein(ngram.at(0), new Script(word1));
				double distance2 = levenshtein(ngram.at(1),new Script(word2));
				double distance3 = levenshtein(ngram.at(2), new Script(word3));
				Couple<Double, Script> suggestion = new Couple<>(distance2, new Script(ngram.at(1)));

				if ((distance1 == 1.0 && distance3 == 1.0) && (distance2 > highestSimilarityTriGram)) {
					suggestionsTriGram.clear();
					suggestionsTriGram.add(suggestion);
					highestSimilarityTriGram = distance2;
				} else if ((distance1 == 1.0 && distance3 == 1.0) && (distance2 == highestSimilarityTriGram)) {
					suggestionsTriGram.add(suggestion);
				}
				if ((distance1 == 1.0 || distance3 == 1.0) && (distance2 > highestSimilarityBiGram)) {
					suggestionsBiGram.clear();
					suggestionsBiGram.add(suggestion);
					highestSimilarityBiGram = distance2;
				} else if ((distance1 == 1.0 || distance3 == 1.0) && (distance2 == highestSimilarityBiGram)){
					suggestionsBiGram.add(suggestion);
				}
				if (distance2 > highestSimilarityDirect) {
					suggestionsDirect.clear(); // Keep only the closest match
					suggestionsDirect.add(suggestion);
					highestSimilarityDirect = distance2;
				} else if (distance2 == highestSimilarityDirect) {
					suggestionsDirect.add(suggestion); // Allow ties
				}

			}
			printInfo(suggestionsTriGram, "TriGram");
			printInfo(suggestionsBiGram, "BiGram");
			printInfo(suggestionsDirect, "Direct");

			Double acceptanceThreshold = 0.70;

			Set<Couple<Double, Script>> suggestions = new HashSet<>();

			if(highestSimilarityTriGram>=acceptanceThreshold) {
				suggestions.addAll(suggestionsTriGram);
			} else if (highestSimilarityBiGram>=acceptanceThreshold) {
				suggestions.addAll(suggestionsBiGram);
			} else if (highestSimilarityDirect>=acceptanceThreshold) {
				suggestions.addAll(suggestionsDirect);
			}

			for (Couple<Double, Script> suggestion : suggestions) {
				System.out.println("Vorschlag: \"" + suggestion.getValue() + "\" mit der Levenshtein Distance: " + suggestion.getKey());
			}
			System.out.println("Test");
		}

		scanner.close();
	}

	private static void printInfo(Set<Couple<Double, Script>> suggestions, String type) {
		if (!suggestions.isEmpty()) {
			for (Couple<Double, Script> suggestion : suggestions) {
				System.out.println(type + " - Vorschlag: \"" + suggestion.getValue() + "\" mit der Levenshtein Distance: " + suggestion.getKey());
			}
		} else {
			System.out.println(type + " - Keine Ergebnisse gefunden");
		}
	}


    // Source: https://wortschatz.uni-leipzig.de/en/download/English
    public static void main(String[] args) {
        String path = "/Users/florianzierer/Desktop/SS24/LingoLibry/Transcripts/";
        String fileName = "eng-com_web-public_2018_1M-sentences";
        double percent = 0.10;
        int nGramLength = 3;
        Texture<Texture<Script>> ngrams = getNgrams(path, fileName, percent, nGramLength);
        consoleListener(ngrams);
    }
}

