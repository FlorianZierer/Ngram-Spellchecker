package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(",\"\"]");
    private static final Pattern NGRAM_PATTERN = Pattern.compile("\\[\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\"\\]");

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

    public static int countSentences(Path jsonFilePath) throws IOException {
        int sentenceCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = SENTENCE_END_PATTERN.matcher(line);
                while (matcher.find()) {
                    sentenceCount++;
                }
            }
        }
        return sentenceCount;
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
        int totalSentences = countSentences(jsonFilePath);
        return (int) Math.ceil((double) totalSentences / epochs);
    }
}