package util;

import java.io.BufferedReader;
import java.io.FileReader;
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
                System.err.println("Directory could not be created: " + e.getMessage());
                throw e;
            }
        }
    }

    public static int countSentences(String filePath) throws IOException {
        int sentenceCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sentenceCount++;
                if (sentenceCount <= 5) {
                    System.out.println("Line " + sentenceCount + ": Content: " + line.substring(0, Math.min(50, line.length())) + "...");
                }
            }
            System.out.println("Total lines/sentences in file: " + sentenceCount);
        }
        return sentenceCount;
    }
}