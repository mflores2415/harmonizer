package harmonizer.controller;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.MidiSystem;

import harmonizer.model.Song;
import harmonizer.player.MusicPlayer;
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
	private final MusicPlayer musicPlayer;
	private final Clock clock;
	private final HarmonizerWindow window;
	private Song song;

	public HarmonizerController(Stage stage) {
		musicPlayer = new MusicPlayer();
		clock = new Clock(musicPlayer);
		window = new HarmonizerWindow(stage, this, chordDisplay, clock);
		musicPlayer.setListener(this::updatePlayback);
	}

	public void show() {
		window.show();
	}

	@Override
	public void importMidi(File file) {
		try {
			Song importedSong = new Song(file.toPath());
			musicPlayer.setSequence(importedSong.getSequence());
			song = importedSong;
			chordDisplay.set("— —", 0);
			window.setSongInfo(song);
			window.setSongLoaded(true, !song.getMap().isEmpty());
			clock.reset();
			updatePlayback(new PlaybackUpdate(0, musicPlayer.getDurationMicroseconds(), false, null, 0));
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
			musicPlayer.pause();
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
		if (musicPlayer.isPlaying()) {
			musicPlayer.pause();
		} else {
			musicPlayer.play();
		}
	}

	@Override
	public void rewind() {
		musicPlayer.rewind();
	}

	@Override
	public void restart() {
		musicPlayer.replay();
	}

	public void closePlayer() {
		musicPlayer.close();
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
