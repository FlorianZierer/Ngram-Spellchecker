import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private final Path jsonFilePath;
    private final Texture<Texture<Script>> ngramsToSearch;
    private final int acceptanceThreshold;
    Texture<Prediction> predictions = new Texture<>();



    public LoadNgramCallable(Path jsonFilePath, Texture<Script> wordsToSearch, int ngrams,int acceptanceThreshold) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = new Texture<>(wordsToSearch.grammy(ngrams));
        this.acceptanceThreshold = acceptanceThreshold;
        wordsToSearch.stream().map(w -> predictions.add(new Prediction(w)));
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        return loadExistingNgrams();
    }

    // Lädt existierende N-Gramme aus einer JSON-Datei
    private Texture<Prediction> loadExistingNgrams() throws IOException {

        String nGramJson = Files.readString(jsonFilePath);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(nGramJson);

        Texture<Texture<Script>> loadedNgrams = new Texture<>(readNgram.asList(d -> new Texture<>(d.asList(inner -> new Script(inner.asString())))));
        loadedNgrams.forEach(this::filterForSuggestions);

        return predictions;

    }

    private void filterForSuggestions(Texture<Script> input){
                for(int i=0;i<ngramsToSearch.extent() ;i++){
                    getSuggestion(ngramsToSearch.at(i),input,i);
        }
    }


    private void getSuggestion(Texture<Script> input, Texture<Script> data, int predictionIndex) {
        if (input.extent() != 3 || data.extent() != 3) {
            return; // Skip if not trigrams
        }
        double distance1 = distance(input.at(0), data.at(0));
        double distance2 = distance(input.at(1), data.at(1));
        double distance3 = distance(input.at(2), data.at(2));

        if (distance1 >= acceptanceThreshold && distance3 >= acceptanceThreshold && distance2 >= acceptanceThreshold) {
            predictions.at(predictionIndex).addSuggestionTriGram(new Suggestion(distance2, data.at(1)));
        }
        if ((distance1 >= acceptanceThreshold || distance3 >= acceptanceThreshold) && distance2 >= acceptanceThreshold) {
            predictions.at(predictionIndex).addSuggestionBiGram(new Suggestion(distance2, data.at(1)));
        }
        if (distance2 >= acceptanceThreshold && distance1 < acceptanceThreshold && distance3 < acceptanceThreshold) {
            predictions.at(predictionIndex).addSuggestionDirect(new Suggestion(distance2, data.at(1)));
        }
    }

    // Berechnet die Levenshtein-Distanz zwischen zwei Wörtern
    static public Double distance(Script word1, Script word2) {
        return word1.similares(word2, Legacy.Similitude.Levenshtein);
    }
}