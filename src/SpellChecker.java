import lingolava.Legacy;
import lingolava.Nexus;
import lingolava.Tuple;
import lingologs.Script;
import lingologs.Texture;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpellChecker {

    private Double acceptanceThreshold;

    Texture<Texture<Script>> ngrams = new Texture<>();

    public SpellChecker(Double acceptanceThreshold) {
        this.acceptanceThreshold = acceptanceThreshold;
    }


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
        Script S = processText(Path.of(path + filename), percent);
        String superScripts = "[⁰¹²³⁴⁵⁶⁷⁸⁹]";
        Pattern toDelete = Pattern.compile("(\\d+\\t)|•|\"" + superScripts); // TODO: to remove », http://cdn. , " , -- ,  ¹, «  / Zitate raus und variation
        Script clearedText = S.replace(toDelete, Script.NIX).toLower();
        return new Texture<>(clearedText.split("\\s+"));
    }

    public void setCorpora(String directoryPath, double percent, Integer nGrammLength) {
        Texture.Builder<Texture<Script>> ngramsBuilder = new Texture.Builder<>();
        try {
            // Hole alle .txt-Dateien im angegebenen Ordner
            Files.list(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .forEach(path -> {
                        Texture<Texture<Script>> fileNgrams = getNgrams(directoryPath,path.getFileName().toString(),percent,nGrammLength);
                        ngramsBuilder.attach(fileNgrams);
                        System.out.println("Mit " + path.getFileName().toString() + " / ngram Liste Länge: " + fileNgrams.extent());
                    });
            Texture<Texture<Script>> ngrams = ngramsBuilder.toTexture();
            System.out.println("Finale ngram Liste Länge: " + ngrams.extent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Texture<Texture<Script>> getNgrams(String path, String filename, double percent, Integer nGrammLength) {
        String nameWithoutSuffix = filename.substring(0, filename.lastIndexOf('.'));
        Path jsonFilePath = Path.of(path + "Json/" + nameWithoutSuffix + ".json");
        Texture<Texture<Script>> wordsGrammyfied;
        try {
            if (Files.exists(jsonFilePath)) {
                String nGramJson = Files.readString(jsonFilePath);
                Nexus.DataNote readNgram = Nexus.DataNote.byJSON(nGramJson);
                wordsGrammyfied = new Texture<>(
                        readNgram.asList(d ->
                                new Texture<>(d.asList(inner ->
                                        new Script(inner.asString())
                                ))
                        )
                );


            } else {
                Nexus.JSONProcessor JP = new Nexus.JSONProcessor();
                Texture<Script> words = readAndFilterTxt(path, filename, percent);
                wordsGrammyfied = new Texture<>(words.grammy(nGrammLength));
                List<List<Script>> convertedList = wordsGrammyfied
                        .stream()
                        .map(texture -> new ArrayList<>(texture.stream().toList()))
                        .collect(Collectors.toList());

                Nexus.DataNote json = Nexus.DataNote.by(convertedList);
                Files.createFile(jsonFilePath);
                String jsonString = JP.present(json);
                Files.writeString(jsonFilePath, jsonString);
            }

            return wordsGrammyfied;
        } catch (Exception E) {
            throw new RuntimeException(E);
        }
    }

    public void method() {
        Set<Tuple.Couple<Double, Script>> suggestionsTriGram = new HashSet<>();
        Set<Tuple.Couple<Double, Script>> suggestionsBiGram= new HashSet<>();
        Set<Tuple.Couple<Double, Script>> suggestionsDirect = new HashSet<>();

        double highestSimilarityTriGram = 0.0;
        double highestSimilarityBiGram = 0.0;
        double highestSimilarityDirect = 0.0;// Looking for the smallest distance
        // Ngram Search
        for (Texture<Script> ngram : ngrams) {
            double distance1 = levenshtein(ngram.at(0), new Script("word1"));
            double distance2 = levenshtein(ngram.at(1),new Script("word2"));
            double distance3 = levenshtein(ngram.at(2), new Script("word3"));
            Tuple.Couple<Double, Script> suggestion = new Tuple.Couple<>(distance2, new Script(ngram.at(1)));

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



        Set<Tuple.Couple<Double, Script>> suggestions = new HashSet<>();

        if(highestSimilarityTriGram>=acceptanceThreshold) {
            suggestions.addAll(suggestionsTriGram);
        } else if (highestSimilarityBiGram>=acceptanceThreshold) {
            suggestions.addAll(suggestionsBiGram);
        } else if (highestSimilarityDirect>=acceptanceThreshold) {
            suggestions.addAll(suggestionsDirect);
        }
    }

    private static void printInfo(Set<Tuple.Couple<Double, Script>> suggestions, String type) {
        if (!suggestions.isEmpty()) {
            for (Tuple.Couple<Double, Script> suggestion : suggestions) {
                System.out.println(type + " - Vorschlag: \"" + suggestion.getValue() + "\" mit der Levenshtein Distance: " + suggestion.getKey());
            }
        } else {
            System.out.println(type + " - Keine Ergebnisse gefunden");
        }
    }
}
