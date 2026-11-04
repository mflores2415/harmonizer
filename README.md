# Harmonizer

Harmonizer is a Java Swing application for analyzing chords in MIDI files and
playing them back alongside the chord display.

## Requirements

- Java 25 or newer
- Maven 3.9 or newer

## Build and test

```shell
mvn test
```

## Source layout

- `model` contains notes, chords, and the loaded song.
- `player` contains MIDI playback.
- `ui` contains Swing windows and displays.
- `controller` connects user actions to the model and player.

The playback controls include play/pause, restart, a 10-second rewind, and a
timeline that can be clicked or dragged to seek through a MIDI file.

## Run

```shell
mvn package
```

```shell
java -jar target/harmonizer-1.0-SNAPSHOT.jar
```

Playback icons are bundled with the application.
