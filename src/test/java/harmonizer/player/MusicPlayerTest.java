package harmonizer.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

class MusicPlayerTest {
	@Test
	void seeksAndClampsPositionToTheSongDuration() throws Exception {
		MusicPlayback player;
		try {
			player = new MusicPlayer();
		} catch (IllegalStateException exception) {
			assumeTrue(false, "MIDI sequencer is not available in this environment");
			return;
		}

		try {
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

			player.seekToMicroseconds(500_000);
			assertTrue(Math.abs(player.getPositionMicroseconds() - 500_000) < 100_000);
			assertEquals("Cmaj Gmaj", player.getPlaybackUpdate().chordText());

			player.seekToMicroseconds(1_500_000);
			assertEquals("Gmaj END", player.getPlaybackUpdate().chordText());

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

}
