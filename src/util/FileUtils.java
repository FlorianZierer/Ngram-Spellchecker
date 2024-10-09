package util;// util.FileUtils.java
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Klasse für Dateioperationen
public class FileUtils {
    // Methode zum Abrufen von JSON-Ordnern in einem Verzeichnis
    public static List<Path> getJsonFolders(Path directoryPath) throws IOException {
        return Files.list(directoryPath)
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    // Methode zum Abrufen von JSON-Dateien in einem Ordner
    public static List<Path> getJsonFiles(Path jsonFolder) throws IOException {
        return Files.list(jsonFolder)
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    // Methode zum Abrufen von TXT-Dateien in einem Verzeichnis
    public static List<Path> getTxtFiles(Path directoryPath) throws IOException {
        return Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    // Methode zum Erstellen eines Verzeichnisses, falls es nicht existiert
    public static void createDirectoryIfNotExists(Path directoryPath) throws IOException {
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                System.err.println("Verzeichnis konnte nicht erstellt werden: " + e.getMessage());
                throw e;
            }
        }
    }

    // Methode zum Zählen der Gesamtanzahl von N-Grammen in einer JSON-Datei
    public static int countTotalNgrams(Path jsonFilePath) throws IOException {
        int totalNgrams = 0;
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            int c;
            int nestingLevel = 0;
            boolean inString = false;
            boolean escape = false;

            while ((c = reader.read()) != -1) {
                char ch = (char) c;

                // Behandlung von Zeichenketten
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

                // Zählen der N-Gramme basierend auf der Verschachtelungsebene
                if (ch == '[') {
                    nestingLevel++;
                } else if (ch == ']') {
                    if (nestingLevel == 2) {
                        // Wir haben gerade ein N-Gram-Array geschlossen
                        totalNgrams++;
                    }
                    nestingLevel--;
                }
            }
        }
        return totalNgrams;
    }
}