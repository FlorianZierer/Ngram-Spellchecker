import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private final Path jsonFilePath;
    private final Texture<Texture<Script>> ngramsToSearch;
    private final double acceptanceThreshold;
    Texture<Prediction> predictions = new Texture<>();



    public LoadNgramCallable(Path jsonFilePath, Texture<Script> wordsToSearch, int ngrams,double acceptanceThreshold) {
        this.jsonFilePath = jsonFilePath;
        Texture<Script> paddedWords = addPadding(wordsToSearch);
        // padding benötigt, weil [ich],[esse],[kuchen],[mit],[chicks] -> [[ich],[esse],[kuchen]], [[esse],[kuchen],[mit]], [[kuchen],[mit],[chicks]]
        this.ngramsToSearch = new Texture<>(paddedWords.grammy(ngrams));
        this.acceptanceThreshold = acceptanceThreshold;
        wordsToSearch.stream().map(w -> predictions.add(new Prediction(w)));
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        return loadExistingNgrams();
    }

    private Texture<Script> addPadding(Texture<Script> wordsToSearch){
            Texture.Builder<Script> paddedWords = new Texture.Builder<>();
            paddedWords.attach(Script.of("")); // Füge null am Anfang hinzu
            paddedWords.attach(wordsToSearch);
            paddedWords.attach(Script.of("")); // Füge null am Ende hinzu
            return paddedWords.toTexture();

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
            return; // Überspringe, wenn nicht Trigramme
        }

        double distance1 = distance(input.at(0), data.at(0));
        double distance2 = distance(input.at(1), data.at(1));
        double distance3 = distance(input.at(2), data.at(2));

        boolean distance1Valid = distance1 >= acceptanceThreshold;
        boolean distance2Valid = distance2 >= acceptanceThreshold;
        boolean distance3Valid = distance3 >= acceptanceThreshold;

        // TriGram-Bedingung
        if (distance1Valid && distance2Valid && distance3Valid) {
            predictions.at(predictionIndex).addSuggestionTriGram(new Suggestion(distance2, data.at(1)));
        }
        // BiGram-Bedingung (eines von distance1 oder distance3 ist gültig)
        else if ((distance1Valid || distance3Valid) && distance2Valid) {
            predictions.at(predictionIndex).addSuggestionBiGram(new Suggestion(distance2, data.at(1)));
        }
        // Direkte Suggestion
        else if (distance2Valid && !distance1Valid && !distance3Valid) {
            predictions.at(predictionIndex).addSuggestionDirect(new Suggestion(distance2, data.at(1)));
        }
    }


    // Berechnet die Levenshtein-Distanz zwischen zwei Wörtern
    static public Double distance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return -1.0;
        }
        return word1.similares(word2, Legacy.Similitude.Levenshtein);
    }
}