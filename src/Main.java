import lingolava.Legacy;
import lingolava.Nexus.*;
import lingologs.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static lingolava.Tuple.*;

public class Main
{


	 public static Script processText(Path path, double percent)
	{
		try
		{
			List<String> L = Files.readAllLines(path);
			L.removeIf(String::isEmpty);
			List<String> Lshort = shortenList(L,percent);
			String U = String.join(" ", Lshort);
            return new Script(U);
		}
		catch (Exception E)
		{
			throw new RuntimeException(E);
		}
	}

	private static List<String> shortenList(List<String> L,double percent){
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


	private static Texture<Script> readAndFilterTxt(String path,String filename, double percent){
		Script S = processText(Path.of(path + filename + ".txt"), percent);
		String superScripts = "[⁰¹²³⁴⁵⁶⁷⁸⁹]";
		Pattern toDelete = Pattern.compile("(\\d+\\t)|•|\""+superScripts); // TODO: to remove », http://cdn. , " , -- ,  ¹, «  / Zitate raus und variation
		Script clearedText = S.replace(toDelete, Script.NIX).toLower();
		return new Texture<>(clearedText.split("\\s+"));
	}

	private static Texture<Texture<Script>> getNgrams(String path, String filename, double percent, Integer nGrammLength) {
		Path jsonFilePath = Path.of(path + "Json/" + filename + ".json");
		Texture<Texture<Script>> wordsGrammyfied;
		try
		{
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


			}else {
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
		}
		catch (Exception E)
		{
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


			List<Couple<Double,Script>> suggestions = new ArrayList<>();
            for (Texture<Script> ngram : ngrams) {
				if (levenshtein(ngram.at(0), new Script(word1)) == 1.0 &&
						levenshtein(ngram.at(2), new Script(word3)) == 1.0) {
					double distance = levenshtein(new Script(word2), ngram.at(1));
					Couple<Double,Script>
							suggestion = new Couple<>(distance, new Script(ngram.at(1)));
					suggestions.add(suggestion);
				}
			}
			suggestions.sort((a, b) -> b.getKey().compareTo(a.getKey()));

			if (suggestions.size() > 1) {
				for (Couple<Double, Script> suggestion : suggestions) {
					System.out.println("Vorschlag: \"" + suggestion.getValue() + "\" mit der Levenshtein Distance: " + suggestion.getKey());
				}
			} else {
				System.out.println("Keine Ergebnisse gefunden");
			}

		}

		scanner.close();
	}
	// Source: https://wortschatz.uni-leipzig.de/en/download/English
	public static void main(String[] args) throws Exception {
		String path = "/Users/florianzierer/Desktop/SS24/LingoLibry/Transcripts/";
		String fileName = "eng-com_web-public_2018_1M-sentences";
		double percent = 0.10;
		int nGramLength = 3;
		Texture<Texture<Script>> ngrams = getNgrams(path, fileName, percent, nGramLength);
		consoleListener(ngrams);
	}
}

