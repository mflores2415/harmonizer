package harmonizer.midi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import org.junit.jupiter.api.Test;

class ChordLyricsTest {
	@Test
	void parsesOnlyHarmonizerChordLyrics() {
		assertEquals(Optional.of(new ChordPair("Cmaj", "Gmaj")), ChordLyrics.parse("Cmaj Gmaj"));
		assertEquals(Optional.of(new ChordPair("Bbmin(m7)", ChordLyrics.END)), ChordLyrics.parse("Bbmin(m7) END"));
		assertTrue(ChordLyrics.parse("Hello world").isEmpty());
		assertTrue(ChordLyrics.parse("Cmaj").isEmpty());
	}

	@Test
	void writesEachChordWithTheNextAndRemovesThemAgainKeepingOtherLyrics() throws Exception {
		Track track = new Sequence(Sequence.PPQ, 480).createTrack();
		track.add(new MidiEvent(Midi.textMessage(Midi.META_LYRIC, "Hello"), 0));
		TreeMap<Long, String> chords = new TreeMap<>();
		chords.put(0L, "Cmaj");
		chords.put(480L, "Amin");

		ChordLyrics.write(chords, track);
		assertEquals(List.of("Hello", "Cmaj Amin", "Amin END"), lyrics(track));

		ChordLyrics.removeFrom(track);
		assertEquals(List.of("Hello"), lyrics(track));
	}

	private static List<String> lyrics(Track track) {
		List<String> lyrics = new ArrayList<>();
		for (int index = 0; index < track.size(); index++) {
			if (track.get(index).getMessage() instanceof MetaMessage message && message.getType() == Midi.META_LYRIC) {
				lyrics.add(Midi.text(message));
			}
		}
		return lyrics;
	}
}
