package harmonizer;

import harmonizer.model.Chord;
import harmonizer.model.Note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ChordTest {
	@Test
	void identifiesCMajorFromPitchClasses() {
		Chord chord = new Chord(Set.of(new Note(60), new Note(64), new Note(67)), false);

		assertEquals("Cmaj", chord.getName());
	}

	@Test
	void identifiesMinorChord() {
		Chord chord = new Chord(Set.of(new Note(60), new Note(63), new Note(67)), false);

		assertEquals("Cmin", chord.getName());
	}

	@Test
	void usesFlatSpellingForFlatKeys() {
		Chord chord = new Chord(Set.of(new Note(61), new Note(65), new Note(68)), true);

		assertEquals("Dbmaj", chord.getName());
	}

	@Test
	void repeatedPitchClassAcrossOctavesDoesNotChangeChord() {
		Set<Note> notes = new HashSet<>();
		notes.add(new Note(60));
		notes.add(new Note(72));
		notes.add(new Note(64));
		notes.add(new Note(67));

		assertEquals(3, notes.size());
		Chord chord = new Chord(notes, false);
		assertEquals("Cmaj", chord.getName());
	}

	@Test
	void chordEqualityUsesItsNameAndHasMatchingHashCode() {
		Chord cMajor = new Chord(Set.of(new Note(60), new Note(64), new Note(67)), false);
		Chord sameChord = new Chord(Set.of(new Note(48), new Note(52), new Note(55)), false);
		Chord cMinor = new Chord(Set.of(new Note(60), new Note(63), new Note(67)), false);

		assertEquals(cMajor, sameChord);
		assertEquals(cMajor.hashCode(), sameChord.hashCode());
		assertNotEquals(cMajor, cMinor);
		assertNotEquals(cMajor, null);
	}
}
