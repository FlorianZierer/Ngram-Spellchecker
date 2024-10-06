# Ngram-Spellchecker
Ein Ngram-basierter Rechtschreibprüfer, der semantische Beziehungen innerhalb von Texten nutzt, um die Genauigkeit der Korrekturen zu verbessern.

## Projektstruktur
Das Projekt besteht aus mehreren Ordnern, die jeweils unterschiedliche Funktionen erfüllen:

### 1. Transcripts-Ordner
In diesem Ordner können Sie `.txt`-Dateien ablegen, die beim Start des Programms in den **Jason-Ordner** konvertiert werden. Befinden sich bereits `.json`-Dateien im Jason-Ordner, werden diese ebenfalls eingelesen. Wenn sowohl im Transcripts- als auch im Txt-Ordner dieselben Dateien vorhanden sind, werden diese nicht erneut konvertiert.

### 2. Jason-Ordner
Dieser Ordner enthält die aus den `.txt`-Dateien generierten `.json`-Dateien. Beim Programmstart wird überprüft, ob sich bereits Dateien im Jason-Ordner befinden. Diese werden dann für die weitere Verarbeitung genutzt.

### 3. Backroom-Ordner
In diesem Ordner finden Sie bereits heruntergeladene `.txt`-Dateien. Diese können Sie einfach in den **Transcripts-Ordner** verschieben, um sie dort zu verarbeiten.

### 4. Evaluation-Ordner
Hier befinden sich Evaluationsdatensätze, die zur Bewertung der Rechtschreibkorrektur verwendet werden können.

## Funktionen und Konfiguration

### 1. Prozentsatz der Datennutzung
Der Prozentsatz der genutzten Datensätze kann in der Konfigurationsdatei angepasst werden. Es wird empfohlen, den Prozentsatz niedrig zu halten, um Speicherfehler zu vermeiden.

### 2. Ngram-Einstellungen
Die Anzahl der Ngrams und der genutzten Threads lässt sich ebenfalls in der Konfigurationsdatei festlegen.

### 3. Spellchecker-Einstellungen
Sie haben die Möglichkeit, den Ngram-basierten Spellchecker optional zu deaktivieren, indem Sie den entsprechenden Codeblock ein- oder auskommentieren. Es ist auch möglich, den Ngram-basierten Rechtschreibprüfer mit einem herkömmlichen wortbasierten Rechtschreibprüfer zu vergleichen.

## Nutzung

- Legen Sie Ihre `.txt`-Dateien in den **Transcripts-Ordner** oder verschieben Sie Dateien aus dem **Backroom-Ordner** in den Transcripts-Ordner.
- Starten Sie das Programm. Die Dateien werden in den **Jason-Ordner** konvertiert und verarbeitet.
- Optional: Passen Sie die Konfigurationsdatei an, um die Nutzung von Ngrams, die Anzahl der Threads oder den Prozentsatz der Datensätze zu verändern.
