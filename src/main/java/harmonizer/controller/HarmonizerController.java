package harmonizer.controller;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.MidiSystem;
import javax.swing.JOptionPane;

import harmonizer.model.Song;
import harmonizer.player.MusicPlayer;
import harmonizer.ui.ChordDisplay;
import harmonizer.ui.Clock;
import harmonizer.ui.EditChordWindow;
import harmonizer.ui.HarmonizerWindow;
import harmonizer.ui.UniqueChordWindow;

public final class HarmonizerController implements HarmonizerWindow.Actions {
	private final ChordDisplay chordDisplay;
	private final Clock clock;
	private final MusicPlayer musicPlayer;
	private final HarmonizerWindow window;
	private Song song;

	public HarmonizerController() {
		chordDisplay = new ChordDisplay();
		clock = new Clock();
		musicPlayer = new MusicPlayer(clock, chordDisplay);
		clock.setMusicPlayer(musicPlayer);
		window = new HarmonizerWindow(chordDisplay, musicPlayer, clock, this);
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
			window.setSongInfo(song);
			window.setSongLoaded(true);
			clock.createClock();
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
			new UniqueChordWindow(song.getChords());
		}
	}

	@Override
	public void editChord() {
		if (song != null) {
			musicPlayer.pause();
			new EditChordWindow(chordDisplay.getTick(), song, chordDisplay.getChords(), chordDisplay::set);
		}
	}

	@Override
	public void closePlayer() {
		musicPlayer.close();
	}

	private void showError(Exception exception) {
		JOptionPane.showMessageDialog(
				window.getFrame(),
				exception.toString(),
				"Unable to process MIDI file",
				JOptionPane.ERROR_MESSAGE);
	}
}
