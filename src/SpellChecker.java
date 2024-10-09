import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;

import java.io.BufferedReader;
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

    List<Path> jsonFolders;

    // Konstruktor mit Akzeptanzschwellenwert
    public SpellChecker(Double acceptanceThreshold) {
        this.acceptanceThreshold = acceptanceThreshold;
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

        jsonFolders = Files.list(jsonDirectoryPath)
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

    public Texture<Prediction> getPredictions(Texture<Script> searchForWords, int threads, int ngrams, double acceptanceThreshold) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> allPredictions = new Texture.Builder<>();

        for (Path jsonFolder : jsonFolders) {
            List<Path> jsonFilePaths = Files.list(jsonFolder)
                    .filter(path -> !path.getFileName().toString().startsWith("._"))
                    .toList();
            for (Path jsonFile : jsonFilePaths) {
                System.out.println("Searching json file: " + jsonFile.getFileName().toString());
                int totalNgrams = countTotalNgrams(jsonFile);
                int ngramsPerThread = totalNgrams / threads;

                Texture<Prediction> predictions = new Texture<>(searchForWords.map(Prediction::new).toList());
                Texture<Prediction> filePredictions = getMultiThreadingMatches(jsonFile, predictions, threads, ngramsPerThread, ngrams, acceptanceThreshold, totalNgrams);
                allPredictions.attach(filePredictions);
            }
        }

        Texture<Prediction> deduplicatedPredictions = condenseList(allPredictions.toTexture());
        deduplicatedPredictions.forEach(Prediction::sort);
        return deduplicatedPredictions;
    }

    private int countTotalNgrams(Path jsonFilePath) throws IOException {
        int totalNgrams = 0;
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            int c;
            int nestingLevel = 0;
            boolean inString = false;
            boolean escape = false;

            while ((c = reader.read()) != -1) {
                char ch = (char) c;

                if (inString) {
                    if (escape) {
                        escape = false;
                    } else if (ch == '\\') {
                        escape = true;
                    } else if (ch == '"') {
                        inString = false;
                    }
                    continue;
                } else {
                    if (ch == '"') {
                        inString = true;
                        continue;
                    }
                }

                if (ch == '[') {
                    nestingLevel++;
                } else if (ch == ']') {
                    if (nestingLevel == 2) {
                        // We've just closed an n-gram array
                        totalNgrams++;
                    }
                    nestingLevel--;
                }
            }
        }
        return totalNgrams;
    }


    private Texture<Script> addPadding(Texture<Script> wordsToSearch) {
        Texture.Builder<Script> paddedWords = new Texture.Builder<>();
        paddedWords.attach(Script.of("")); // Füge null am Anfang hinzu
        paddedWords.attach(wordsToSearch);
        paddedWords.attach(Script.of("")); // Füge null am Ende hinzu
        return paddedWords.toTexture();

    }

    // Parallele Verarbeitung von Dateien zur N-Gramm-Extraktion
    public Texture<Prediction> getMultiThreadingMatches(Path jsonFilePath, Texture<Prediction> predictions, int threads, int ngramsPerThread, int ngrams, double acceptanceThreshold, int totalNgrams) throws ExecutionException, InterruptedException, IOException {
        Texture.Builder<Prediction> predictionBuilder = new Texture.Builder<>();
        Texture<Script> paddedWords = addPadding(predictions.map(Prediction::getWord));
        List<LoadNgramCallable> NGC = new ArrayList<>();
        for (int threadID = 0; threadID < threads; threadID++) {
            int startNgramIndex = ngramsPerThread * threadID;
            int endNgramIndex = (threadID == threads - 1) ? totalNgrams : startNgramIndex + ngramsPerThread;
            NGC.add(new LoadNgramCallable(jsonFilePath, predictions, paddedWords, ngrams, acceptanceThreshold, startNgramIndex, endNgramIndex));
        }

        ExecutorService ExSe = Executors.newFixedThreadPool(threads);

        List<Future<Texture<Prediction>>> future = NGC.stream()
                .map(ExSe::submit).toList();

        future.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).forEach(predictionBuilder::attach);

        ExSe.shutdown();
        return condenseList(predictionBuilder.toTexture());
    }

    public Texture<Prediction> condenseList(Texture<Prediction> predictions) {

        List<Script> predictionsNames = new ArrayList<>();
        List<Prediction> condenseList = new ArrayList<>();

        for (Prediction prediction : predictions) {
            if (!(predictionsNames.contains(prediction.getWord()))) {
                condenseList.add(prediction);
                predictionsNames.add(prediction.getWord());
            } else {
                for (Prediction existingPrediction : condenseList) {
                    if (existingPrediction.getWord().equals(prediction.getWord())) {
                        existingPrediction.setSuggestionsTriGram(
                                mergeSuggestions(existingPrediction.getSuggestionsTriGram(), prediction.getSuggestionsTriGram()));
                        existingPrediction.setSuggestionsBiGram(
                                mergeSuggestions(existingPrediction.getSuggestionsBiGram(), prediction.getSuggestionsBiGram()));
                        existingPrediction.setSuggestionsDirect(
                                mergeSuggestions(existingPrediction.getSuggestionsDirect(), prediction.getSuggestionsDirect()));
                        break;
                    }
                }
            }
        }
        return new Texture<>(condenseList);
    }

    // Methode zum Kombinieren von Vorschlägen
    private Texture<Suggestion> mergeSuggestions(Texture<Suggestion> suggestions1, Texture<Suggestion> suggestions2) {
        List<Suggestion> mergedList = new ArrayList<>();

        if (suggestions1 != null) {
            mergedList.addAll(suggestions1.toList());
        }

        if (suggestions2 != null) {
            for (Suggestion suggestion : suggestions2) {
                boolean found = false;
                for (Suggestion existingSuggestion : mergedList) {
                    if (Objects.equals(suggestion.getScript(), existingSuggestion.getScript())) {
                        existingSuggestion.merge(suggestion);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    mergedList.add(suggestion);
                }
            }
        }
        return new Texture<>(mergedList);
    }


    // Setzt das Korpus aus Dateien in einem Verzeichnis
    public void setCorpora(Path directoryPath, double percent, Integer nGramLength, int threads, int epochs) {
        try {
            getNgrams(directoryPath, nGramLength, threads, percent, epochs);

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}