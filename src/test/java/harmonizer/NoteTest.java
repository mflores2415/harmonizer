package harmonizer;

import harmonizer.model.Note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class NoteTest {
	@Test
	void notesInDifferentOctavesShareTheirPitchClass() {
		Note middleC = new Note(60);
		Note higherC = new Note(72);

		assertEquals(middleC, higherC);
		assertEquals(middleC.hashCode(), higherC.hashCode());
	}

	@Test
	void notesInDifferentPitchClassesRemainDistinct() {
		assertNotEquals(new Note(60), new Note(61));
	}

	@Test
	void noteSetContainsOnlyOneEntryPerPitchClass() {
		Set<Note> notes = new HashSet<>();
		notes.add(new Note(60));
		notes.add(new Note(72));

		assertEquals(1, notes.size());
	}

	@Test
	void negativePitchesUsePositivePitchClasses() {
		assertEquals(11, new Note(-1).getRelativePitch());
	}
}
