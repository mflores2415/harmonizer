package harmonizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyDetectorTest {
	// G major: I–IV–V–I with a scale melody.
	private static final int[][] G_MAJOR_CHORDS = {{55, 59, 62}, {60, 64, 67}, {62, 66, 69}, {55, 59, 62}};
	private static final int[] G_MAJOR_MELODY = {67, 69, 71, 72, 74, 76, 78, 79};
	// A minor (harmonic): i–iv–V–i.
	private static final int[][] A_MINOR_CHORDS = {{57, 60, 64}, {62, 65, 69}, {64, 68, 71}, {57, 60, 64}};
	private static final int[] A_MINOR_MELODY = {69, 71, 72, 74, 76, 77, 80, 81};

	@TempDir
	Path tempDir;

	@Test
	void detectsMajorAndMinorKeys() throws Exception {
		KeyDetector.Key g = KeyDetector.detect(song(G_MAJOR_CHORDS, G_MAJOR_MELODY, null)).orElseThrow();
		assertEquals("G major", g.name());
		assertFalse(g.flat());

		KeyDetector.Key a = KeyDetector.detect(song(A_MINOR_CHORDS, A_MINOR_MELODY, null)).orElseThrow();
		assertEquals("A minor", a.name());
	}

	@Test
	void ignoresDrumsAndReturnsEmptyWithoutPitchedNotes() throws Exception {
		Sequence drums = new Sequence(Sequence.PPQ, 480);
		Track track = drums.createTrack();
		note(track, 9, 36, 0, 240);
		note(track, 9, 38, 240, 480);
		assertTrue(KeyDetector.detect(drums).isEmpty());
	}

	@Test
	void estimatesKeyWhenFileHasNone() throws Exception {
		Song song = Song.load(write("no-key.mid", song(G_MAJOR_CHORDS, G_MAJOR_MELODY, null)));
		assertEquals("G major", song.getKey().name());
		assertTrue(song.getKey().estimated());
	}

	@Test
	void keepsDeclaredKeyThatFits() throws Exception {
		// One sharp, major = G major.
		Song song = Song.load(write("g-key.mid", song(G_MAJOR_CHORDS, G_MAJOR_MELODY, new byte[] {1, 0})));
		assertEquals("G major", song.getKey().name());
		assertFalse(song.getKey().estimated());
	}

	@Test
	void replacesDefaultCMajorThatDoesNotFit() throws Exception {
		// E major (four sharps) labelled with a default C major signature.
		int[][] chords = {{52, 56, 59}, {57, 61, 64}, {59, 63, 66}, {52, 56, 59}};
		int[] melody = {64, 66, 68, 69, 71, 73, 75, 76};
		Song song = Song.load(write("bogus-c.mid", song(chords, melody, new byte[] {0, 0})));
		assertEquals("E major", song.getKey().name());
		assertTrue(song.getKey().estimated());
	}

	@Test
	void switchesToRelativeMinorWhenItFitsBetter() throws Exception {
		// A minor music labelled as C major: same signature, wrong mode.
		Song song = Song.load(write("relative.mid", song(A_MINOR_CHORDS, A_MINOR_MELODY, new byte[] {0, 0})));
		assertEquals("A minor", song.getKey().name());
		assertTrue(song.getKey().estimated());
	}

	private static Sequence song(int[][] chords, int[] melody, byte[] keySignature) throws Exception {
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		Track track = sequence.createTrack();
		if (keySignature != null) {
			MetaMessage key = new MetaMessage();
			key.setMessage(0x59, keySignature, keySignature.length);
			track.add(new MidiEvent(key, 0));
		}
		for (int bar = 0; bar < chords.length; bar++) {
			for (int pitch : chords[bar]) {
				note(track, 0, pitch, bar * 1920L, (bar + 1) * 1920L);
			}
		}
		for (int step = 0; step < melody.length; step++) {
			note(track, 1, melody[step], step * 960L, (step + 1) * 960L);
		}
		return sequence;
	}

	private static void note(Track track, int channel, int pitch, long start, long end) throws Exception {
		ShortMessage on = new ShortMessage();
		on.setMessage(ShortMessage.NOTE_ON, channel, pitch, 100);
		track.add(new MidiEvent(on, start));
		ShortMessage off = new ShortMessage();
		off.setMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
		track.add(new MidiEvent(off, end));
	}

	private Path write(String name, Sequence sequence) throws Exception {
		Path file = tempDir.resolve(name);
		MidiSystem.write(sequence, 1, file.toFile());
		return file;
	}
}
