package generator;

import constants.Constants;
import lingologs.Script;
import lingologs.Texture;
import util.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NgramGenerator {
    // Hauptmethode zur Generierung von N-Grammen
    public static void generateNgrams(Path directoryPath, int nGramLength, int threads, double percent, int epochs) throws IOException, ExecutionException, InterruptedException {
        // Erstellen des JSON-Verzeichnisses
        Path jsonDirectoryPath = directoryPath.resolve("Json");
        FileUtils.createDirectoryIfNotExists(jsonDirectoryPath);

        // Abrufen von TXT-Dateien und JSON-Ordnern
        List<Path> txtFiles = FileUtils.getTxtFiles(directoryPath);
        List<Path> jsonFolders = FileUtils.getJsonFolders(jsonDirectoryPath);

        // Extrahieren der JSON-Ordnernamen
        List<String> jsonNames = jsonFolders.stream()
                .map(folder -> folder.getFileName().toString())
                .toList();

        // Verarbeitung jeder TXT-Datei
        for (Path txtFile : txtFiles) {
            String fileName = txtFile.getFileName().toString();
            String nameWithoutSuffix = fileName.substring(0, fileName.lastIndexOf('.'));

            // Überprüfen, ob bereits ein JSON für diese Datei existiert
            if (!jsonNames.contains(nameWithoutSuffix)) {
                try {
                    // Erstellen von N-Grammen mit mehreren Threads
                    createMultiThreaded(directoryPath, fileName, nGramLength, percent, threads, epochs);
                } catch (Exception e) {
                    System.err.println(Constants.ANSI_RED + "Fehler bei der Verarbeitung von N-Grammen für TXT-Datei: " + fileName + Constants.ANSI_RESET);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Methode zur Erstellung von N-Grammen mit mehreren Threads
    private static Texture<Texture<Script>> createMultiThreaded(Path directoryPath, String filename, int nGramLength,
                                                                double percent, int threads, int epochs) throws ExecutionException, InterruptedException, IOException {
        Texture.Builder<Texture<Script>> ngramsBuilder = new Texture.Builder<>();
        Path filePath = directoryPath.resolve(filename);

        // Berechnung der zu verarbeitenden Zeilen
        long totalLines = Files.lines(filePath, StandardCharsets.UTF_8).count();
        int lines = (int) (totalLines * percent);
        int batchSize = lines / epochs;
        int batchPerThread = batchSize / threads;

        // Verarbeitung in Epochen
        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println(filename + " wird in Epoche " + epoch + " geladen" + Constants.ANSI_BLUE);
            Texture<Texture<Script>> epochResult = createEpochMultiThreaded(filePath, nGramLength, batchPerThread, threads, epoch);
            ngramsBuilder.attach(epochResult);
        }
        System.out.println(filename + " JSON-Erstellung abgeschlossen" + Constants.ANSI_PURPLE);
        return ngramsBuilder.toTexture();
    }

    // Methode zur Erstellung von N-Grammen für eine Epoche mit mehreren Threads
    private static Texture<Texture<Script>> createEpochMultiThreaded(Path filePath, int nGramLength,
                                                                     int batchPerThread, int threads, int epochNumber) throws ExecutionException, InterruptedException {
        Texture.Builder<Texture<Script>> epochBuilder = new Texture.Builder<>();
        List<CreateNgramCallable> ngramCallables = new ArrayList<>();

        // Erstellen von Callable-Objekten für jeden Thread
        for (int threadID = 0; threadID < threads; threadID++) {
            int start = batchPerThread * epochNumber * threadID;
            int end = start + batchPerThread;
            ngramCallables.add(new CreateNgramCallable(filePath, start, end, nGramLength, threadID));
        }

        // Ausführen der Threads und Sammeln der Ergebnisse
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Future<Texture<Texture<Script>>>> futures = ngramCallables.stream()
                .map(executorService::submit)
                .toList();

        for (Future<Texture<Texture<Script>>> future : futures) {
            try {
                epochBuilder.attach(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        executorService.shutdown();
        return epochBuilder.toTexture();
    }
}