package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String processBatch(String batchContent, boolean isFirstBatch, boolean isLastBatch) {
        if (!isFirstBatch) {
            batchContent = batchContent.replaceFirst("^,", "[");
        }
        if (!isLastBatch) {
            batchContent = batchContent.replaceFirst(",$", "]");
        }
        return batchContent;
    }

    public static int calculateBatchSize(Path jsonFilePath, int epochs) throws IOException {
        int totalSentences = countSentences(String.valueOf(jsonFilePath));
        return (int) Math.ceil((double) totalSentences / epochs);
    }

    public static int countSentences(String filePath) throws IOException {
        int sentenceCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sentenceCount += countOccurrences(line, "\"\",\"\"");
            }
        }
        return sentenceCount;
    }

    private static int countOccurrences(String str, String subStr) {
        int count = 0;
        int lastIndex = 0;
        while (lastIndex != -1) {
            lastIndex = str.indexOf(subStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += subStr.length();
            }
        }
        return count;
    }

    public static List<String> readBatch(BufferedReader reader, int batchSize) throws IOException {
        List<String> batch = new ArrayList<>();
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null && count < batchSize) {
            batch.add(line);
            count++;
        }
        return batch;
    }
}