package harmonizer.controller;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;

import harmonizer.midi.Midi;
import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.Song;
import harmonizer.player.MusicPlayback;
import harmonizer.player.PlaybackUpdate;
import harmonizer.ui.ChordDisplay;
import harmonizer.ui.Clock;
import harmonizer.ui.HarmonizerWindow;
import harmonizer.ui.UniqueChordWindow;
import javafx.application.Platform;
import javafx.stage.Stage;

/** Connects the window to the song and the player: carries out what the user asks for. */
public final class HarmonizerController implements HarmonizerWindow.Actions {
	private final ChordDisplay chordDisplay = new ChordDisplay();
	private final MusicPlayback player;
	private final Clock clock;
	private final HarmonizerWindow window;
	private Song song;
	private ChordAnalysisMode analysisMode = ChordAnalysisMode.DETAILED;

	public HarmonizerController(Stage stage, MusicPlayback player) {
		this.player = Objects.requireNonNull(player, "player");
		clock = new Clock(player);
		window = new HarmonizerWindow(stage, this, chordDisplay, clock);
		chordDisplay.setOnCorrectChordName(this::correctChordName);
		clock.setUpdateListener(update -> {
			if (song != null) {
				window.showPlaybackTick(update.tick(), update.playing());
			}
		});
		player.setListener(this::showPlayback);
	}

	public void show() {
		window.show();
	}

	public void closePlayer() {
		player.close();
	}

	@Override
	public void openSong(File file) {
		try {
			Song loaded = Song.load(file.toPath(), analysisMode);
			player.setSpeed(1.0);
			player.setSequence(loaded.getSequence());
			song = loaded;
			chordDisplay.showEmptyState();
			clock.reset();
			window.showSong(song);
			window.showSpeed(player.getSpeed(), songTempo());
			showPlayback(player.getPlaybackUpdate());
		} catch (Exception exception) {
			window.showError(exception);
		}
	}

	@Override
	public void saveSong(File file) {
		if (song == null) {
			return;
		}
		try {
			if (MidiSystem.write(song.getSequence(), 0, file) == 0) {
				throw new IOException("No MIDI file type is available for this song.");
			}
		} catch (IOException exception) {
			window.showError(exception);
		}
	}

	@Override
	public void showUniqueChords() {
		if (song != null) {
			new UniqueChordWindow(window.getStage(), song.getUniqueChords()).show();
		}
	}

	@Override
	public void correctChordName() {
		if (song == null || !song.hasChords()) {
			return;
		}
		player.pause();
		long chordTick = song.getChordTickAt(player.getPlaybackUpdate().tick());
		String currentName = song.getChordsByTick().get(chordTick).getName();
		int bar = song.getBarGrid().barAt(chordTick);
		window.askChordName(currentName, song.getKey().flat(), bar)
				.filter(name -> !name.equals(currentName))
				.ifPresent(name -> changeSong(changing -> changing.renameChord(chordTick, name)));
	}

	@Override
	public void transpose(int semitones) {
		if (song == null || semitones == song.getTranspose()) {
			return;
		}
		int corrections = song.getCorrectedChordCount();
		if (corrections > 0 && !window.confirmTransposeDiscardsCorrections(corrections)) {
			return;
		}
		changeSong(changing -> changing.setTranspose(semitones));
	}

	@Override
	public void changeAnalysisMode(ChordAnalysisMode mode) {
		if (mode == analysisMode) {
			return;
		}
		analysisMode = mode;
		if (song != null && !changeSong(changing -> changing.setAnalysisMode(mode))) {
			analysisMode = song.getAnalysisMode();
			window.setAnalysisMode(analysisMode);
		}
	}

	@Override
	public void changeSpeed(double speed) {
		player.setSpeed(speed);
		window.showSpeed(player.getSpeed(), songTempo());
	}

	@Override
	public void changeInstrumentView(boolean enabled) {
		window.setInstrumentView(enabled);
	}

	@Override
	public void playPause() {
		if (player.isPlaying()) {
			player.pause();
		} else {
			player.play();
		}
	}

	@Override
	public void rewind() {
		player.rewind();
	}

	@Override
	public void restart() {
		player.replay();
	}

	/**
	 * Applies a change to the song, then reloads it into the player at the same position
	 * (still playing if it was) and refreshes the window.
	 *
	 * @return false if the change failed (the error has been shown)
	 */
	private boolean changeSong(SongChange change) {
		long position = player.getPositionMicroseconds();
		boolean wasPlaying = player.isPlaying();
		try {
			change.apply(song);
			player.setSequence(song.getSequence());
			player.seekToMicroseconds(position);
			window.showSong(song);
			if (wasPlaying) {
				player.play();
			}
			showPlayback(player.getPlaybackUpdate());
			return true;
		} catch (Exception exception) {
			window.showError(exception);
			return false;
		}
	}

	private int songTempo() {
		return song != null && song.getTempo() > 0 ? song.getTempo() : Midi.DEFAULT_TEMPO_BPM;
	}

	/** Called by the player, possibly off the JavaFX thread. */
	private void showPlayback(PlaybackUpdate update) {
		Platform.runLater(() -> {
			if (update.chords() != null) {
				chordDisplay.show(update.chords());
			}
			clock.update(update);
		});
	}

	@FunctionalInterface
	private interface SongChange {
		void apply(Song song) throws InvalidMidiDataException;
	}
}
