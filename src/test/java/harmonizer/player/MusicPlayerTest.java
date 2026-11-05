package harmonizer.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.junit.jupiter.api.Test;

import harmonizer.midi.ChordLyrics;
import harmonizer.midi.Midi;

/** Needs a working MIDI sequencer; skipped on machines without one (e.g. CI servers). */
class MusicPlayerTest {
	@Test
	void seeksAndClampsPositionToTheSongDuration() throws Exception {
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		Track track = sequence.createTrack();
		ShortMessage noteOn = new ShortMessage();
		noteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 100);
		ShortMessage noteOff = new ShortMessage();
		noteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
		track.add(new MidiEvent(noteOn, 0));
		track.add(new MidiEvent(noteOff, 1_920));
		track.add(new MidiEvent(Midi.textMessage(Midi.META_LYRIC, "Hello world"), 0));
		track.add(new MidiEvent(Midi.textMessage(Midi.META_LYRIC, "Cmaj Gmaj"), 0));
		track.add(new MidiEvent(Midi.textMessage(Midi.META_LYRIC, "Gmaj END"), 960));

		try (MusicPlayback player = playerWith(sequence)) {
			player.seekToMicroseconds(500_000);
			assertTrue(Math.abs(player.getPositionMicroseconds() - 500_000) < 100_000);
			assertEquals("Cmaj Gmaj", ChordLyrics.format(player.getPlaybackUpdate().chords()));

			player.seekToMicroseconds(1_500_000);
			assertEquals("Gmaj END", ChordLyrics.format(player.getPlaybackUpdate().chords()));

			player.seekToMicroseconds(-1);
			assertTrue(player.getPositionMicroseconds() < 100_000);

			player.seekToMicroseconds(Long.MAX_VALUE);
			assertTrue(Math.abs(player.getPositionMicroseconds() - player.getDurationMicroseconds()) < 100_000);
		}
	}

	@Test
	void speedIsClampedAndKeptWhenTheSequenceChanges() throws Exception {
		try (MusicPlayback player = playerWith(new Sequence(Sequence.PPQ, 480))) {
			player.setSpeed(0.5);
			player.setSequence(new Sequence(Sequence.PPQ, 480));
			assertEquals(0.5, player.getSpeed(), 1e-9);
			player.setSpeed(10);
			assertEquals(MusicPlayer.MAX_SPEED, player.getSpeed(), 1e-9);
			player.setSpeed(0);
			assertEquals(MusicPlayer.MIN_SPEED, player.getSpeed(), 1e-9);
		}
	}

	/** A player with {@code sequence} loaded, or the test is skipped if MIDI isn't available. */
	private static MusicPlayback playerWith(Sequence sequence) throws Exception {
		MusicPlayer player;
		try {
			player = new MusicPlayer();
		} catch (IllegalStateException exception) {
			return abort("MIDI sequencer is not available: " + exception.getMessage());
		}
		try {
			player.setSequence(sequence);
			return player;
		} catch (MidiUnavailableException exception) {
			player.close();
			return abort("MIDI sequencer could not be opened: " + exception.getMessage());
		}
	}
}
