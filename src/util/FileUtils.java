package util;// util.FileUtils.java
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileUtils {
    public static List<Path> getJsonFolders(Path directoryPath) throws IOException {
        return Files.list(directoryPath)
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    public static List<Path> getJsonFiles(Path jsonFolder) throws IOException {
        return Files.list(jsonFolder)
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    public static List<Path> getTxtFiles(Path directoryPath) throws IOException {
        return Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .toList();
    }

    public static void createDirectoryIfNotExists(Path directoryPath) throws IOException {
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
                throw e;
            }
        }
    }

    public static int countTotalNgrams(Path jsonFilePath) throws IOException {
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
}
