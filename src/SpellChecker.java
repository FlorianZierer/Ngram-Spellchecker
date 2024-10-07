import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SpellChecker {

    // Schwellenwert für die Akzeptanz von Wortähnlichkeiten
    private Double acceptanceThreshold;

    // Pfad zum Evaluierungsdatensatz
    private static final String EVALUATION_DATASET_PATH = "./Transcripts/Evaluation/evaluation_dataset.json";

    // Konstruktor mit Akzeptanzschwellenwert
    public SpellChecker(Double acceptanceThreshold) {
        this.acceptanceThreshold = acceptanceThreshold;
    }

    // Berechnet die Levenshtein-Distanz zwischen zwei Wörtern
    static public Double distance(Script word1, Script word2) {
        return word1.similares(word2, Legacy.Similitude.Levenshtein);
    }

    public static Texture<Texture<Script>> multiThreadingCreate(Path directoryPath, String filename, int nGramLength,
                                                                double percent, int threads, int epochs) throws ExecutionException, InterruptedException, IOException {
        Texture.Builder<Texture<Script>> ngramsBuilder = new Texture.Builder<>();
        Path filePath = directoryPath.resolve(filename);
        long totalLines = Files.lines(filePath, StandardCharsets.UTF_8).count();
        int lines = (int) (totalLines * percent);
        int batchSize = lines / epochs;

        int batchProThread = batchSize / threads;

        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println(filename + " wird in der Epoche " + epoch + " geladen" + Constants.ANSI_BLUE);
            Texture<Texture<Script>> epochResult = multiThreadingCreateEpoch(filePath, nGramLength, batchProThread, threads, epoch);
            ngramsBuilder.attach(epochResult);
        }
        System.out.println(filename + " JsonErstellung abgeschlossen" + Constants.ANSI_PURPLE);
        return ngramsBuilder.toTexture();
    }

    // Parallele Verarbeitung von Dateien zur N-Gramm-Extraktion für eine Epoche
    private static Texture<Texture<Script>> multiThreadingCreateEpoch(Path filePath, int nGramLength,
                                                                      int batchProThread, int threads, int epochNumber) throws ExecutionException, InterruptedException {
        Texture.Builder<Texture<Script>> epochBuilder = new Texture.Builder<>();


        List<CreateNgramCallable> NGC = new ArrayList<>();
        for (int threadID = 0; threadID < threads; threadID++) {
            int start = batchProThread * epochNumber * threadID;
            int end = start + batchProThread;
            NGC.add(new CreateNgramCallable(filePath, start, end, nGramLength, threadID));
        }

        ExecutorService ExSe = Executors.newFixedThreadPool(threads);

        List<Future<Texture<Texture<Script>>>> future = NGC.stream()
                .map(ExSe::submit).toList();

        future.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(epochBuilder::attach);

        ExSe.shutdown();

        return epochBuilder.toTexture();
    }

    // Parallele Verarbeitung von Dateien zur N-Gramm-Extraktion
    public static Texture<Texture<Script>> multiThreadingLoad(Path jsonFilePath,double percent,int threads) throws ExecutionException, InterruptedException, IOException {
        Texture.Builder<Texture<Script>> ngramsBuilder = new Texture.Builder<>();

        long fileSize = Files.size(jsonFilePath);
        long processSize = (long) (fileSize * percent);
        int chunkSize = (int) (processSize / threads);

        List<LoadNgramCallable> NGC = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int start = i * chunkSize;
            int end = (i == threads - 1) ? (int) processSize : (i + 1) * chunkSize;
            NGC.add(new LoadNgramCallable(jsonFilePath));
        }

        ExecutorService ExSe = Executors.newFixedThreadPool(threads);

        List<Future<Texture<Texture<Script>>>> future = NGC.stream()
                .map(ExSe::submit).toList();

        future.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(ngramsBuilder::attach);

        ExSe.shutdown();

        return ngramsBuilder.toTexture();
    }

    // Setzt das Korpus aus Dateien in einem Verzeichnis
    public void setCorpora(Path directoryPath, double percent, Integer nGramLength, int threads, int epochs) {
        try {
            getNgrams(directoryPath, nGramLength, threads, percent, epochs);

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Extrahiert N-Gramme aus Dateien im angegebenen Verzeichnis
    public void getNgrams(Path directoryPath, Integer nGramLength, int threads, double percent, int epochs) throws ExecutionException, InterruptedException, IOException {

        Path jsonDirectoryPath = directoryPath.resolve("Json");

        if (!Files.exists(jsonDirectoryPath)) {
            try {
                Files.createDirectories(jsonDirectoryPath);
            } catch (IOException e) {
                System.err.println("Failed to create Json directory: " + e.getMessage());
            }
        }

        List<Path> txtFiles = Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();

        List<Path> jsonFolders = Files.list(jsonDirectoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();

        List<String> jsonNames = jsonFolders
                .stream()
                .map(folder -> folder.getFileName().toString())
                .toList();


        // Verarbeite TXT-Dateien, wenn keine entsprechende JSON existiert
        for (Path txtFile : txtFiles) {
            String fileName = txtFile.getFileName().toString();
            String nameWithoutSuffix = fileName.substring(0, fileName.lastIndexOf('.'));

            if (!jsonNames.contains(nameWithoutSuffix)) {
                try {
                    multiThreadingCreate(directoryPath, fileName, nGramLength, percent, threads, epochs);
                } catch (Exception E) {
                    System.err.println(Constants.ANSI_RED + "Fehler bei der Verarbeitung von N-Grammen für TXT-Datei: " + fileName + Constants.ANSI_RESET);
                    throw new RuntimeException(E);
                }
            }
        }
    }



    // Korrigiert ein Wort basierend auf dem Kontext (vorheriges und nachfolgendes Wort)
    public String getCorrection(String word1, String word2, String word3, boolean directOnly) {
        Map<String, Suggestion> suggestionsTriGram = new HashMap<>();
        Map<String, Suggestion> suggestionsBiGram = new HashMap<>();
        Map<String, Suggestion> suggestionsDirect = new HashMap<>();

        double highestSimilarityTriGram = 0.0;
        double highestSimilarityBiGram = 0.0;
        double highestSimilarityDirect = 0.0;

        for (Texture<Script> ngram : ngrams) {
            double distance1 = word1 == null ? -1 : distance(ngram.at(0), new Script(word1));
            double distance2 = distance(ngram.at(1), new Script(word2));
            double distance3 = word3 == null ? -1 : distance(ngram.at(2), new Script(word3));
            Script script = new Script(ngram.at(1));
            String scriptString = script.toString();

            if (!directOnly) {
                if (word1 != null && word3 != null) {
                    if ((distance1 >= acceptanceThreshold && distance3 >= acceptanceThreshold) && (distance2 > highestSimilarityTriGram)) {
                        suggestionsTriGram.clear();
                        suggestionsTriGram.put(scriptString, new Suggestion(distance2, script));
                        highestSimilarityTriGram = distance2;
                    } else if ((distance1 >= acceptanceThreshold && distance3 >= acceptanceThreshold) && (distance2 == highestSimilarityTriGram)) {
                        updateSuggestion(suggestionsTriGram, scriptString, distance2, script);
                    }
                }

                if ((distance1 >= acceptanceThreshold || distance3 >= acceptanceThreshold) && (distance2 > highestSimilarityBiGram)) {
                    suggestionsBiGram.clear();
                    suggestionsBiGram.put(scriptString, new Suggestion(distance2, script));
                    highestSimilarityBiGram = distance2;
                } else if ((distance1 >= acceptanceThreshold || distance3 >= acceptanceThreshold) && (distance2 == highestSimilarityBiGram)) {
                    updateSuggestion(suggestionsBiGram, scriptString, distance2, script);
                }
            }

            if (distance2 > highestSimilarityDirect) {
                suggestionsDirect.clear();
                suggestionsDirect.put(scriptString, new Suggestion(distance2, script));
                highestSimilarityDirect = distance2;
            } else if (distance2 == highestSimilarityDirect) {
                updateSuggestion(suggestionsDirect, scriptString, distance2, script);
            }
        }

        if (!directOnly) {
            printInfo(suggestionsTriGram, "TriGram");
            printInfo(suggestionsBiGram, "BiGram");
        }
        printInfo(suggestionsDirect, "Direct");

        Map<String, Suggestion> suggestions = new HashMap<>();

        if (directOnly) {
            if (highestSimilarityDirect >= acceptanceThreshold) {
                suggestions.putAll(suggestionsDirect);
            }
        } else {
            if (highestSimilarityTriGram >= acceptanceThreshold) {
                suggestions.putAll(suggestionsTriGram);
            } else if (highestSimilarityBiGram >= acceptanceThreshold) {
                suggestions.putAll(suggestionsBiGram);
            } else if (highestSimilarityDirect >= acceptanceThreshold) {
                suggestions.putAll(suggestionsDirect);
            }
        }

        if (suggestions.isEmpty()) {
            return word2;
        }

        return suggestions.values().stream()
                .max(Comparator.comparingDouble((Suggestion s) -> s.score)
                        .thenComparingInt(s -> s.repetitionCount))
                .map(s -> s.script.toString())
                .orElse(word2);
    }

    // Aktualisiert die Vorschläge und deren Wiederholungszähler
    private void updateSuggestion(Map<String, Suggestion> suggestions, String scriptString, double score, Script script) {
        if (suggestions.containsKey(scriptString)) {
            suggestions.get(scriptString).incrementRepetitionCount();
        } else {
            suggestions.put(scriptString, new Suggestion(score, script));
        }
    }

    // Gibt Informationen zu den Vorschlägen aus
    private void printInfo(Map<String, Suggestion> suggestions, String category) {
        System.out.println(category + " Vorschläge:");
        suggestions.forEach((script, suggestion) ->
                System.out.println(script + " (Punktzahl: " + suggestion.score + ", Wiederholungen: " + suggestion.repetitionCount + ")"));
    }
}