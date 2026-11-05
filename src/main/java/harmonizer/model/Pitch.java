package harmonizer.model;

import java.util.List;

/** Names for pitches and pitch classes. */
public final class Pitch {
	private static final List<String> SHARP_NAMES = List.of(
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
	private static final List<String> FLAT_NAMES = List.of(
			"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B");

	private Pitch() {
	}

	/** The pitch class (0 = C … 11 = B) of a MIDI pitch. */
	public static int pitchClass(int pitch) {
		return Math.floorMod(pitch, 12);
	}

	/** A pitch class spelled with sharps or flats, e.g. {@code 1 → "C#"} or {@code "Db"}. */
	public static String name(int pitchClass, boolean flat) {
		return names(flat).get(pitchClass(pitchClass));
	}

	/** All twelve pitch-class names from C, spelled with sharps or flats. */
	public static List<String> names(boolean flat) {
		return flat ? FLAT_NAMES : SHARP_NAMES;
	}

	/** A MIDI pitch with its octave, e.g. {@code 60 → "C4"}. */
	public static String withOctave(int pitch) {
		return name(pitch, false) + (Math.floorDiv(pitch, 12) - 1);
	}
}
