import lingolava.Legacy;
import lingologs.Script;
import lingologs.Texture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

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



    private Texture<Prediction> getFittingNgrams(Texture<Script> searchForWords, int threads, int ngrams,int acceptanceThreshold) throws IOException, ExecutionException, InterruptedException {

        Texture.Builder<Prediction> textureBuilder = new Texture.Builder<>();
        for(Path jsonFolder : jsonFolders){
            List<Path> jsonFilePaths = Files
                    .list(jsonFolder)
                    .filter(path -> !path.getFileName().toString().startsWith("._"))
                    .toList();
            for(Path jsonFile : jsonFilePaths){
                int totalLines = (int) Files.lines(jsonFile, StandardCharsets.UTF_8).count();
                int batchProThread = totalLines / (jsonFilePaths.size()+1);
                textureBuilder.attach(getMultiThreadingMatches(jsonFile,searchForWords,threads,batchProThread,ngrams,acceptanceThreshold));
            }

        }
        return textureBuilder.toTexture();
    }

    // Parallele Verarbeitung von Dateien zur N-Gramm-Extraktion
    public Texture<Prediction> getMultiThreadingMatches(Path jsonFilePath, Texture<Script> searchForWords, int threads, int batchProThread, int ngrams,int acceptanceThreshold) throws ExecutionException, InterruptedException, IOException {


        Texture.Builder<Prediction> matching = new Texture.Builder<>();

        List<LoadNgramCallable> NGC = new ArrayList<>();
        for (int threadID = 0; threadID < threads; threadID++) {
            int start = batchProThread * threadID;
            int end = start + batchProThread;
            NGC.add(new LoadNgramCallable(jsonFilePath,searchForWords,ngrams,acceptanceThreshold));
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
                })
                .forEach(matching::attach);

        ExSe.shutdown();

       return matching.toTexture();
    }

    // Setzt das Korpus aus Dateien in einem Verzeichnis
    public void setCorpora(Path directoryPath, double percent, Integer nGramLength, int threads, int epochs) {
        try {
            getNgrams(directoryPath, nGramLength, threads, percent, epochs);

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
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
                System.out.println(script + " Punktzahl: " + ", Wiederholungen: "));
    }
}