import lingolava.Legacy;
import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class LoadNgramCallable implements Callable<Texture<Prediction>> {
    private final Path jsonFilePath;
    private final Texture<Texture<Script>> ngramsToSearch;
    private final double acceptanceThreshold;
    private final int startNgramIndex;
    private final int endNgramIndex;
    private final int ngramSize;
    private final List<Prediction> mutablePredictions;

    public LoadNgramCallable(Path jsonFilePath, Texture<Prediction> predictions, Texture<Script> paddedWords, int ngrams, double acceptanceThreshold, int startNgramIndex, int endNgramIndex) {
        this.jsonFilePath = jsonFilePath;
        this.ngramsToSearch = new Texture<>(paddedWords.grammy(ngrams));
        this.acceptanceThreshold = acceptanceThreshold;
        this.startNgramIndex = startNgramIndex;
        this.endNgramIndex = endNgramIndex;
        this.ngramSize = ngrams;
        this.mutablePredictions = new ArrayList<>(predictions.toList());
    }

    @Override
    public Texture<Prediction> call() throws Exception {
        loadExistingNgrams();
        return new Texture<>(mutablePredictions);
    }

    // Load existing N-grams from a JSON file incrementally
    private void loadExistingNgrams() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            int c = reader.read();
            // Skip any whitespace
            while (Character.isWhitespace(c)) {
                c = reader.read();
            }
            if (c != '[') {
                throw new IOException("Expected '[' at the beginning of the JSON array");
            }

            int ngramIndex = 0;
            boolean endOfFile = false;

            // Read n-gram arrays one by one
            while (!endOfFile) {
                // Skip any whitespace or commas
                do {
                    c = reader.read();
                    if (c == -1) {
                        endOfFile = true;
                        break;
                    }
                } while (Character.isWhitespace(c) || c == ',');

                if (endOfFile || c == ']') {
                    // End of the top-level array
                    break;
                }

                if (c != '[') {
                    throw new IOException("Expected '[' at the beginning of n-gram array");
                }

                StringBuilder ngramBuilder = new StringBuilder();
                ngramBuilder.append((char) c);
                int nestingLevel = 1;
                boolean inString = false;
                boolean escape = false;

                while (nestingLevel > 0) {
                    c = reader.read();
                    if (c == -1) {
                        throw new IOException("Unexpected end of file");
                    }
                    char ch = (char) c;
                    ngramBuilder.append(ch);

                    if (inString) {
                        if (escape) {
                            escape = false;
                        } else if (ch == '\\') {
                            escape = true;
                        } else if (ch == '"') {
                            inString = false;
                        }
                    } else {
                        if (ch == '"') {
                            inString = true;
                        } else if (ch == '[') {
                            nestingLevel++;
                        } else if (ch == ']') {
                            nestingLevel--;
                        }
                    }
                }

                if (ngramIndex >= startNgramIndex && ngramIndex < endNgramIndex) {
                    // Now ngramBuilder contains the n-gram array as a String
                    String ngramString = ngramBuilder.toString();
                    Nexus.DataNote ngramNote = Nexus.DataNote.byJSON(ngramString);
                    Texture<Script> ngram = new Texture<>(ngramNote.asList(inner -> new Script(inner.asString())));
                    filterForSuggestions(ngram);
                }

                ngramIndex++;

                if (ngramIndex >= endNgramIndex) {
                    break;
                }
            }
        }
    }

    private void filterForSuggestions(Texture<Script> inputNgram) {
        for (int i = 0; i < ngramsToSearch.extent(); i++) {
            getSuggestion(ngramsToSearch.at(i), inputNgram, i);
        }
    }

    private void getSuggestion(Texture<Script> input, Texture<Script> data, int predictionIndex) {
        if (input.extent() != ngramSize || data.extent() != ngramSize) {
            return; // Skip if not matching n-gram size
        }

        double distance1 = distance(input.at(0), data.at(0));
        double distance2 = distance(input.at(1), data.at(1));
        double distance3 = distance(input.at(2), data.at(2));

        boolean distance1Valid = distance1 >= acceptanceThreshold;
        boolean distance2Valid = distance2 >= acceptanceThreshold;
        boolean distance3Valid = distance3 >= acceptanceThreshold;

        if (distance1Valid && distance2Valid && distance3Valid) {
            mutablePredictions.get(predictionIndex).addSuggestionTriGram(new Suggestion(distance2, data.at(1)));
        } else if ((distance1Valid || distance3Valid) && distance2Valid) {
            mutablePredictions.get(predictionIndex).addSuggestionBiGram(new Suggestion(distance2, data.at(1)));
        } else if (distance2Valid && !distance1Valid && !distance3Valid) {
            mutablePredictions.get(predictionIndex).addSuggestionDirect(new Suggestion(distance2, data.at(1)));
        }
    }


    // Calculates the Levenshtein distance between two words
    static public Double distance(Script word1, Script word2) {
        if (word1.toString().isEmpty() || word2.toString().isEmpty()) {
            return -1.0;
        }
        return word1.similares(word2, Legacy.Similitude.Cosine);
    }
}
