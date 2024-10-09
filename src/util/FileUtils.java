package util;

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
    // Diese Methode ist speichereffizient und verarbeitet die Datei zeichenweise
    public static int countTotalNgrams(Path jsonFilePath) throws IOException {
        int totalNgrams = 0;
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            int nestingLevel = 0;
            boolean inString = false;
            boolean escape = false;
            int c;

            // Lese die Datei zeichenweise
            while ((c = reader.read()) != -1) {
                char ch = (char) c;

                // Verarbeite Zeichen innerhalb von Strings
                if (inString) {
                    if (escape) {
                        escape = false;
                    } else if (ch == '\\') {
                        escape = true;
                    } else if (ch == '"') {
                        inString = false;
                    }
                } else {
                    // Verarbeite Zeichen außerhalb von Strings
                    if (ch == '"') {
                        inString = true;
                    } else if (ch == '[') {
                        nestingLevel++;
                        // Zähle N-Gramme auf der zweiten Verschachtelungsebene
                        if (nestingLevel == 2) {
                            totalNgrams++;
                        }
                    } else if (ch == ']') {
                        nestingLevel--;
                    }
                }
            }
        }
        return totalNgrams;
    }
}