package generator;

import constants.Constants;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Diese Klasse implementiert Callable und ist für die Erstellung von N-Grammen verantwortlich
public class CreateNgramCallable implements Callable<Texture<Texture<Script>>> {
    private final Path filePath;
    private final int start;
    private final int end;
    private final int nGramLength;
    private static final int BUFFER_SIZE = 10000;
    private final Path jsonDirectoryPath;
    private final Path directoryPath;
    private final String filename;

    // Konstruktor für die Klasse
    public CreateNgramCallable(Path filePath, int start, int end,
                               int nGramLength, int threadID) {
        this.filePath = filePath;
        this.start = start;
        this.end = end;
        this.nGramLength = nGramLength;
        this.filename = filePath.getFileName().toString().substring(0, filePath.getFileName().toString().lastIndexOf('.'));
        this.directoryPath = filePath.getParent().resolve("Json").resolve(filename);
        this.jsonDirectoryPath = directoryPath.resolve(filename + "_" + threadID + ".json");
    }

    // Überschriebene call-Methode, die von Callable gefordert wird
    @Override
    public Texture<Texture<Script>> call() throws Exception {
        createAndSaveNewNgrams();
        return new Texture<>();
    }

    // Erstellt und speichert neue N-Gramme aus einer Textdatei
    private void createAndSaveNewNgrams() throws IOException, ExecutionException, InterruptedException {
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                System.err.println("Failed to create Json directory: " + e.getMessage());
            }
        }
        if (!Files.exists(jsonDirectoryPath)) {
            try {
                Files.createFile(jsonDirectoryPath);
            } catch (IOException e) {
                System.err.println("Failed to create Json directory: " + e.getMessage());
            }
        }
        Texture<Texture<Script>> fileNGrams = createNgrams();
        appendNewNgrams(fileNGrams);
    }

    // Fügt neue N-Gramme zur JSON-Datei hinzu
    private static final String JSON_PART_SEPARATOR = "###JSON_PART###";

    private void appendNewNgrams(Texture<Texture<Script>> newNGrams) throws IOException {
        Nexus.JSONProcessor JP = new Nexus.JSONProcessor();
        List<List<Script>> convertedList = newNGrams
                .stream()
                .map((texture) -> new ArrayList<>(texture.stream().toList()))
                .collect(Collectors.toList());

        Nexus.DataNote json = Nexus.DataNote.by(convertedList);
        String jsonString = JP.present(json);

        try (RandomAccessFile file = new RandomAccessFile(jsonDirectoryPath.toFile(), "rw")) {
            long length = file.length();
            if (length > 0) {
                // Move to the end of the file
                file.seek(length);
                // Add the new JSON part and then the separator
                file.writeBytes(JSON_PART_SEPARATOR + jsonString);
            } else {
                // If the file is empty, write the JSON string with a separator at the end
                file.writeBytes(jsonString);
            }
        }
    }

    private Texture<Texture<Script>> createNgrams() throws IOException {
        List<Texture<Script>> sentences = readAndFilterTxt();
        Texture.Builder<Texture<Script>> ngramsBuilder = new Texture.Builder<>();

        for (Texture<Script> sentence : sentences) {
            Texture<Script> paddedSentence = addPadding(sentence);
            ngramsBuilder.attach(new Texture<>(paddedSentence.grammy(nGramLength)));
        }

        return ngramsBuilder.toTexture();
    }

    private List<Texture<Script>> readAndFilterTxt() throws IOException {
        Pattern toDelete = Pattern.compile("\\d+\\t|(https?:)?\\w+\\.\\w{2,3}|\\s+-\\s+|[^a-z'&\\- ]");
        List<Texture<Script>> sentences = new ArrayList<>();
        Texture.Builder<Script> sentenceBuilder = new Texture.Builder<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int currentLine = 0;

            // Skip to the start line
            while (currentLine < start && reader.readLine() != null) {
                currentLine++;
            }

            // Read lines from start to end
            while ((line = reader.readLine()) != null && currentLine < end) {
                currentLine++;
                if (!line.isEmpty()) {
                    String processedLine = toDelete.matcher(line.toLowerCase()).replaceAll("");
                    String[] splitWords = processedLine.split("\\s+");
                    for (String word : splitWords) {
                        if (!word.isEmpty()) {
                            sentenceBuilder.attach(new Script(word));
                        }
                    }

                    // Each line is considered a sentence, so we add it to the list and start a new one
                    if (!sentenceBuilder.isEmpty()) {
                        sentences.add(sentenceBuilder.toTexture());
                        sentenceBuilder = new Texture.Builder<>();
                    }
                }
            }

            // Add the last sentence if it's not empty
            if (!sentenceBuilder.isEmpty()) {
                sentences.add(sentenceBuilder.toTexture());
            }
        } catch (IOException e) {
            System.err.println(Constants.ANSI_RED + "Error reading and filtering text" + Constants.ANSI_RESET);
            throw new RuntimeException(e);
        }
        return sentences;
    }

    private Texture<Script> addPadding(Texture<Script> sentence) {
        Texture.Builder<Script> paddedSentence = new Texture.Builder<>();
        paddedSentence.attach(Script.of(""));
        paddedSentence.attach(sentence);
        paddedSentence.attach(Script.of(""));
        return paddedSentence.toTexture();
    }

    // Verarbeitet den Buffer und fügt die Wörter zum Builder hinzu
    private void processBuffer(List<String> buffer, Texture.Builder<Script> builder) {
        for (String word : buffer) {
            builder.attach(new Script(word));
        }
    }
}