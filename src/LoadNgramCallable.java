import lingolava.Nexus;
import lingologs.Script;
import lingologs.Texture;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class LoadNgramCallable implements Callable<Texture<Texture<Script>>> {
    private final Path jsonFilePath;


    public LoadNgramCallable(Path jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    @Override
    public Texture<Texture<Script>> call() throws Exception {
        return loadExistingNgrams();
    }

    // Lädt existierende N-Gramme aus einer JSON-Datei
    private Texture<Texture<Script>> loadExistingNgrams() throws IOException {
        System.out.println("Lade existierende N-Gramme aus: " + jsonFilePath);
        long startTime = System.nanoTime();
        String nGramJson = Files.readString(jsonFilePath);
        Nexus.DataNote readNgram = Nexus.DataNote.byJSON(nGramJson);
        Texture<Texture<Script>> wordsGrammyfied = new Texture<>(
                readNgram.asList(d ->
                        new Texture<>(d.asList(inner ->
                                new Script(inner.asString())
                        ))
                )
        );
        long endTime = System.nanoTime();
        System.out.println(Constants.ANSI_GREEN + "N-Gramme geladen in " + ((endTime - startTime) / 1_000_000_000.0) + " Sekunden." + Constants.ANSI_RESET);
        return wordsGrammyfied;
    }
}