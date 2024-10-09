package core;

import generator.NgramGenerator;
import generator.PredictionGenerator;
import lingologs.Script;
import lingologs.Texture;
import model.Prediction;
import util.FileUtils;
import util.PredictionUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SpellChecker {
    private final double acceptanceThreshold;
    private List<Path> jsonFolders;

    public SpellChecker(double acceptanceThreshold) {
        this.acceptanceThreshold = acceptanceThreshold;
    }

    public void setCorpora(Path directoryPath, double percent, int nGramLength, int threads, int epochs) {
        try {
            NgramGenerator.generateNgrams(directoryPath, nGramLength, threads, percent, epochs);
            this.jsonFolders = FileUtils.getJsonFolders(directoryPath.resolve("Json"));
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Texture<Prediction> getPredictions(Texture<Script> searchForWords, int threads, int ngrams, boolean directModeEnabled) throws IOException, ExecutionException, InterruptedException {
        Texture.Builder<Prediction> allPredictions = new Texture.Builder<>();

        for (Path jsonFolder : jsonFolders) {
            List<Path> jsonFilePaths = FileUtils.getJsonFiles(jsonFolder);
            for (Path jsonFile : jsonFilePaths) {
                if(directModeEnabled){System.out.println("Searching json file: " + jsonFile.getFileName().toString());}
                Texture<Prediction> filePredictions = PredictionGenerator.generatePredictions(jsonFile, searchForWords, threads, ngrams, acceptanceThreshold, directModeEnabled);
                allPredictions.attach(filePredictions);
            }
        }

        return PredictionUtils.deduplicateAndSortPredictions(allPredictions.toTexture());
    }
}