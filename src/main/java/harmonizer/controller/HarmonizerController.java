package harmonizer.controller;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.sound.midi.MidiSystem;

import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.Song;
import harmonizer.player.MusicPlayback;
import harmonizer.player.PlaybackUpdate;
import harmonizer.ui.ChordDisplay;
import harmonizer.ui.Clock;
import harmonizer.ui.EditChordWindow;
import harmonizer.ui.HarmonizerWindow;
import harmonizer.ui.UniqueChordWindow;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class HarmonizerController implements HarmonizerWindow.Actions {
	private final ChordDisplay chordDisplay = new ChordDisplay();
	private final MusicPlayback musicPlayback;
	private final Clock clock;
	private final HarmonizerWindow window;
	private Song song;
	private ChordAnalysisMode analysisMode = ChordAnalysisMode.DETAILED;

	public HarmonizerController(Stage stage, MusicPlayback musicPlayback) {
		this.musicPlayback = Objects.requireNonNull(musicPlayback, "musicPlayback");
		clock = new Clock(musicPlayback);
		window = new HarmonizerWindow(stage, this, chordDisplay, clock);
		musicPlayback.setListener(this::updatePlayback);
	}

	public void show() {
		window.show();
	}

	@Override
	public void importMidi(File file) {
		try {
			Song importedSong = new Song(file.toPath(), analysisMode);
			musicPlayback.setSequence(importedSong.getSequence());
			song = importedSong;
			chordDisplay.set("— —", 0);
			window.setSongInfo(song);
			window.setSongLoaded(true, !song.getMap().isEmpty());
			clock.reset();
			updatePlayback(new PlaybackUpdate(0, musicPlayback.getDurationMicroseconds(), false, null, 0));
		} catch (Exception exception) {
			showError(exception);
		}
	}

	@Override
	public void saveMidi(File file) {
		if (song == null) {
			return;
		}
		try {
			int fileType = MidiSystem.write(song.getSequence(), 0, file);
			if (fileType == 0) {
				throw new IOException("No MIDI file type is available for this song.");
			}
		} catch (IOException exception) {
			showError(exception);
		}
	}

	@Override
	public void showUniqueChords() {
		if (song != null) {
			new UniqueChordWindow(window.getStage(), song.getChords()).show();
		}
	}

	@Override
	public void editChord() {
		if (song != null && !song.getMap().isEmpty()) {
			musicPlayback.pause();
			EditChordWindow.show(
					window.getStage(),
					song,
					chordDisplay.getTick(),
					chordDisplay.getChords(),
					chordDisplay::set);
		}
	}

	@Override
	public void playPause() {
		if (musicPlayback.isPlaying()) {
			musicPlayback.pause();
		} else {
			musicPlayback.play();
		}
	}

	@Override
	public void rewind() {
		musicPlayback.rewind();
	}

	@Override
	public void restart() {
		musicPlayback.replay();
	}

	@Override
	public void changeAnalysisMode(ChordAnalysisMode mode) {
		if (mode == analysisMode) {
			return;
		}
		if (song == null) {
			analysisMode = mode;
			window.setAnalysisMode(mode);
			return;
		}

		long position = musicPlayback.getPositionMicroseconds();
		boolean wasPlaying = musicPlayback.isPlaying();
		try {
			song.setAnalysisMode(mode);
			analysisMode = mode;
			musicPlayback.setSequence(song.getSequence());
			musicPlayback.seekToMicroseconds(position);
			window.setSongLoaded(true, !song.getMap().isEmpty());
			clock.reset();
			if (wasPlaying) {
				musicPlayback.play();
			}
			updatePlayback(musicPlayback.getPlaybackUpdate());
		} catch (Exception exception) {
			window.setAnalysisMode(analysisMode);
			showError(exception);
		}
	}

	public void closePlayer() {
		musicPlayback.close();
	}

	private void updatePlayback(PlaybackUpdate update) {
		Platform.runLater(() -> {
			if (update.chordText() != null) {
				chordDisplay.set(update.chordText(), update.chordTick());
			}
			clock.update(update);
		});
	}

	private void showError(Exception exception) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.initOwner(window.getStage());
		alert.setTitle("MIDI error");
		alert.setHeaderText("Unable to process MIDI file");
		alert.setContentText(exception.toString());
		alert.showAndWait();
	}
}
