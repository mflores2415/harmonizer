package harmonizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KeySignatureTest {
	@Test
	void namesKeysAndFindsTheirTonic() {
		assertEquals("C major", new KeySignature(0, false).name());
		assertEquals("A minor", new KeySignature(0, true).name());
		assertEquals("Eb major", new KeySignature(-3, false).name());
		assertEquals(3, new KeySignature(-3, false).tonic());
		assertEquals(9, new KeySignature(0, true).tonic());
		assertEquals(new KeySignature(2, true), new KeySignature(2, false).relative());
	}

	@Test
	void transposingPicksTheSpellingWithFewestAccidentals() {
		KeySignature cMajor = new KeySignature(0, false);
		assertEquals("D major", cMajor.transpose(2).name());
		assertEquals("Db major", cMajor.transpose(1).name());
		assertEquals("B major", cMajor.transpose(-1).name());
		assertEquals(cMajor, cMajor.transpose(12));
	}

	@Test
	void rejectsImpossibleSignatures() {
		assertThrows(IllegalArgumentException.class, () -> new KeySignature(8, false));
	}
}
