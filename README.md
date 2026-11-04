# Harmonizer

Harmonizer is a JavaFX desktop application for analyzing chords in MIDI files
and playing them back alongside the chord display.

## Requirements

- Java 25 or newer
- Maven 3.9 or newer
- JavaFX 25 (resolved by Maven)

## Build and test

```shell
mvn test
```

## Source layout

- `model` contains notes, chords, and song state; `MidiSongAnalyzer` imports MIDI,
  reads metadata, identifies chords, and writes the generated chord lyrics.
- `player` contains MIDI playback.
- `ui` contains JavaFX windows, controls, and displays.
- `controller` connects user actions to the model and player.

The playback controls include play/pause, restart, a 10-second rewind, and a
timeline that can be clicked or dragged to seek through a MIDI file.
Use **Chord analysis** to switch between Detailed mode, which shows every
recognized chord-quality change, and Steady mode, which keeps the first chord
quality until the root changes. The selected mode can be changed while a song
is loaded; playback position and chord edits are preserved.

## Run

```shell
mvn javafx:run
```

In VS Code, use the integrated terminal and run `mvn javafx:run` for the
JavaFX module-path launch. Running `Harmonizer.java` directly with the Java
extension uses a classpath launch and can print JavaFX/JDK native-access
warnings; those warnings do not prevent the application from running.

The interface uses JavaFX controls styled with CSS. MIDI playback remains
implemented with the Java Sound API.
