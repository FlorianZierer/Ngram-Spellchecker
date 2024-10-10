package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public static int getEpochs(Path jsonFilePath) throws IOException {
        Pattern jsonPartPattern = Pattern.compile("###JSON_PART###");
        String content = Files.readString(jsonFilePath);
        Matcher jsonPartMatcher = jsonPartPattern.matcher(content);

        int count = 0;
        while (jsonPartMatcher.find()) {
            count++;
        }

        // Return count + 1 (number of parts is one more than number of separators)
        return count+1;
    }
}