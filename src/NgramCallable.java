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
import java.util.regex.Pattern;

class NgramCallable implements Callable<Texture<Texture<Script>>> {
    private final Path filePath;
    private final int start;
    private final int end;
    private final int nGramLength;
    private final double percent;
    private static final int BUFFER_SIZE = 10000;

    public NgramCallable(Path filePath, int start, int end, int nGramLength, double percent) {
        this.filePath = filePath;
        this.start = start;
        this.end = end;
        this.nGramLength = nGramLength;
        this.percent = percent;
    }

    @Override
    public Texture<Texture<Script>> call() throws Exception {
        return createNgrams();
    }

    private Texture<Texture<Script>> createNgrams() throws IOException {
        System.out.println(Constants.ANSI_YELLOW + "Erstelle neue n-Gramme von Zeile " + start + " bis " + end + " (Verarbeite " + (percent * 100) + "% der Datei)" + Constants.ANSI_RESET);
        Texture<Script> words = readAndFilterTxt();
        return new Texture<>(words.grammy(nGramLength));
    }

    private Texture<Script> readAndFilterTxt() throws IOException {
        System.out.println(Constants.ANSI_CYAN + "Lese und filtere Text von Zeile " + start + " bis " + end + Constants.ANSI_RESET);
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

                // Prüfe, ob der gewünschte Prozentsatz der Datei verarbeitet wurde
                if (currentLine >= end * percent) {
                    break;
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