# Harmonizer

Harmonizer is a JavaFX desktop application for analyzing chords in MIDI and
Karaoke MIDI files and playing them back alongside the chord display.

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
Karaoke `.kar` files can be opened alongside `.mid` and `.midi` files; their
existing lyrics are preserved, while Harmonizer displays its generated chord
labels during playback.
Use **View → Instruments** to choose an instrument part and see its notes on a
piano roll: a fixed playhead with notes scrolling past, a keyboard that lights
up the sounding pitches, and drum parts labeled by drum name. Scroll over the
roll to zoom. **View → Chords** returns to the chord cards.

**Speed** (in the player bar) slows playback down to practice: 25%, 50%, 75%,
100%, 125% or 150%, or **Custom tempo…** to type any BPM from 10% to 200% of
the song's tempo. Pitch stays the same and the saved file is not changed.

**Transpose** (next to the song details, or **Song → Transpose**, Ctrl+Up/Down)
shifts every pitched note of the song up or down by semitones. This changes
what you hear, the detected chords and key, and what gets saved; drums are not
affected.

**Chords → Correct chord name…** (or right-click the chord cards) fixes a chord
the detector named wrongly. It only changes the name shown and saved with the
file, not the music. Corrections are cleared when the song is transposed.

Use **Chords → Chord detection** to switch between Detailed mode, which shows
every recognized chord-quality change, and Steady mode, which keeps the first
chord quality until the root changes. The selected mode can be changed while a
song is loaded; playback position and chord corrections are preserved.

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
