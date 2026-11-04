package harmonizer.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import harmonizer.ui.ChordDisplay;
import harmonizer.ui.Clock;

class MusicPlayerTest {
	@Test
	void seeksAndClampsPositionToTheSongDuration() throws Exception {
		Clock clock = new Clock();
		ChordDisplay chordDisplay = new ChordDisplay();
		MusicPlayer player;
		try {
			player = new MusicPlayer(clock, chordDisplay);
		} catch (IllegalStateException exception) {
			assumeTrue(false, "MIDI sequencer is not available in this environment");
			return;
		}

		try {
			JPanel controls = player.createPlayer();
			JButton playButton = findButton(controls, "playPauseButton");
			assertTrue(playButton.isVisible());
			assertEquals("Play", playButton.getText());
			assertTrue(!playButton.isEnabled());

			Sequence sequence = new Sequence(Sequence.PPQ, 480);
			ShortMessage noteOn = new ShortMessage();
			noteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 100);
			ShortMessage noteOff = new ShortMessage();
			noteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
			var track = sequence.createTrack();
			track.add(new MidiEvent(noteOn, 0));
			track.add(new MidiEvent(noteOff, 1_920));
			addLyric(track, "Cmaj Gmaj", 0);
			addLyric(track, "Gmaj END", 960);
			player.setSequence(sequence);
			SwingUtilities.invokeAndWait(() -> {
			});
			assertTrue(playButton.isVisible());
			assertTrue(playButton.isEnabled());

			player.seekToMicroseconds(500_000);
			assertTrue(Math.abs(player.getPositionMicroseconds() - 500_000) < 100_000);
			assertEquals("Cmaj Gmaj", chordDisplay.getChords());

			player.seekToMicroseconds(1_500_000);
			assertEquals("Gmaj END", chordDisplay.getChords());

			player.seekToMicroseconds(-1);
			assertTrue(player.getPositionMicroseconds() < 100_000);

			player.seekToMicroseconds(Long.MAX_VALUE);
			assertTrue(Math.abs(player.getPositionMicroseconds() - player.getDurationMicroseconds()) < 100_000);
		} finally {
			player.close();
		}
	}

	private void addLyric(javax.sound.midi.Track track, String text, long tick) throws Exception {
		byte[] data = text.getBytes(StandardCharsets.UTF_8);
		MetaMessage lyric = new MetaMessage();
		lyric.setMessage(5, data, data.length);
		track.add(new MidiEvent(lyric, tick));
	}

	private JButton findButton(JPanel panel, String name) {
		for (java.awt.Component component : panel.getComponents()) {
			if (component instanceof JButton button && name.equals(button.getName())) {
				return button;
			}
		}
		throw new AssertionError("Play/pause button was not added to the player controls");
	}
}
