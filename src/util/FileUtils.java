package util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Utility-Klasse für Dateioperationen
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

    // Methode zum Zählen der Epochen in einer JSON-Datei
    public static int getEpochs(Path jsonFilePath) throws IOException {
        Pattern jsonPartPattern = Pattern.compile("###JSON_PART###");
        String content = Files.readString(jsonFilePath);
        Matcher jsonPartMatcher = jsonPartPattern.matcher(content);

        int count = 0;
        while (jsonPartMatcher.find()) {
            count++;
        }

        // Gibt count + 1 zurück (Anzahl der Teile ist eins mehr als die Anzahl der Trennzeichen)
        return count+1;
    }
}