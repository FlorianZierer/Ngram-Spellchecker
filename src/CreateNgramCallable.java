import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class CreateNgramCallable implements Callable<Texture<Texture<Script>>> {
    private final Path filePath;
    private final int start;
    private final int end;
    private final int nGramLength;
    private static final int BUFFER_SIZE = 10000;
    private final Path jsonDirectoryPath;
    private final String filename;
    private final int thread;


    public CreateNgramCallable(Path filePath, int start, int end,
                               int nGramLength, int threadID) {
        this.filePath = filePath;
        this.start = start;
        this.end = end;
        this.nGramLength = nGramLength;
        this.jsonDirectoryPath = filePath.getParent().resolve("Json")
                .resolve(filePath.getFileName().toString().substring(0, filePath.getFileName().toString().lastIndexOf('.')) + "_" + threadID + ".json");

        this.filename = filePath.getFileName().toString();
        this.thread = threadID;
    }

    @Override
    public Texture<Texture<Script>> call() throws Exception {
        System.out.println("thread nummer " + thread + "schreibt in " + jsonDirectoryPath);
        return createAndSaveNewNgrams();
    }

    // Erstellt und speichert neue N-Gramme aus einer Textdatei
    private Texture<Texture<Script>> createAndSaveNewNgrams() throws IOException, ExecutionException, InterruptedException {

        Nexus.JSONProcessor JP = new Nexus.JSONProcessor();
        Texture<Texture<Script>> fileNGrams = createNgrams();

        List<List<Script>> convertedList = fileNGrams
                .stream()
                .map((texture) -> new ArrayList<>(texture.stream().toList()))
                .collect(Collectors.toList());

        Nexus.DataNote json = Nexus.DataNote.by(convertedList);
        if (!Files.exists(jsonDirectoryPath)) {
            try {
                Files.createFile(jsonDirectoryPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String jsonString = JP.present(json);
        Files.writeString(jsonDirectoryPath, jsonString);
        return fileNGrams;
    }

    private Texture<Texture<Script>> createNgrams() throws IOException {
        Texture<Script> words = readAndFilterTxt();
        return new Texture<>(words.grammy(nGramLength));
    }

    private Texture<Script> readAndFilterTxt() throws IOException {
        Pattern toDelete = Pattern.compile("\\d+\\t|(https?:)?\\w+\\.\\w{2,3}|\\s+-\\s+|[^a-z'&\\- ]");
        Texture.Builder<Script> builder = new Texture.Builder<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int currentLine = 0;
            List<String> buffer = new ArrayList<>();

            // Überspringe bis zur Startzeile
            while (currentLine < start && reader.readLine() != null) {
                currentLine++;
            }

            // Lese Zeilen von Start bis Ende
            while ((line = reader.readLine()) != null && currentLine < end) {
                currentLine++;
                if (!line.isEmpty()) {
                    String processedLine = toDelete.matcher(line.toLowerCase()).replaceAll("");
                    String[] splitWords = processedLine.split("\\s+");
                    for (String word : splitWords) {
                        if (!word.isEmpty()) {
                            buffer.add(word);
                        }
                    }

                    // Verarbeite Buffer, wenn BUFFER_SIZE erreicht ist
                    if (buffer.size() >= BUFFER_SIZE) {
                        processBuffer(buffer, builder);
                        buffer.clear();
                    }
                }
            }

            // Verarbeite verbleibende Wörter im Buffer
            if (!buffer.isEmpty()) {
                processBuffer(buffer, builder);
            }
        } catch (IOException e) {
            System.err.println(Constants.ANSI_RED + "Fehler beim Lesen und Filtern des Textes" + Constants.ANSI_RESET);
            throw new RuntimeException(e);
        }
        return builder.toTexture();
    }

    private void processBuffer(List<String> buffer, Texture.Builder<Script> builder) {
        for (String word : buffer) {
            builder.attach(new Script(word));
        }
    }
}