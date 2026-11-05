package harmonizer.model;

import java.util.Optional;

import javax.sound.midi.MetaMessage;

import harmonizer.midi.Midi;

/**
 * A MIDI key signature: a number of sharps (positive) or flats (negative), and major or minor.
 */
public record KeySignature(int accidentals, boolean minor) {
	private static final String[] MAJOR_NAMES = {
			"Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#"
	};
	private static final String[] MINOR_NAMES = {
			"Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#"
	};

	public KeySignature {
		if (accidentals < -7 || accidentals > 7) {
			throw new IllegalArgumentException("accidentals must be between -7 and 7: " + accidentals);
		}
	}

	/** Reads a key-signature meta event, or empty if it is malformed. */
	public static Optional<KeySignature> from(MetaMessage message) {
		byte[] data = message.getData();
		if (message.getType() != Midi.META_KEY_SIGNATURE || data.length < 2) {
			return Optional.empty();
		}
		int accidentals = data[0];
		int mode = Byte.toUnsignedInt(data[1]);
		if (accidentals < -7 || accidentals > 7 || mode > 1) {
			return Optional.empty();
		}
		return Optional.of(new KeySignature(accidentals, mode == 1));
	}

	public byte[] toData() {
		return new byte[] {(byte) accidentals, (byte) (minor ? 1 : 0)};
	}

	/** Pitch class of the key's tonic (0 = C). */
	public int tonic() {
		int majorTonic = Math.floorMod(7 * accidentals, 12);
		return minor ? (majorTonic + 9) % 12 : majorTonic;
	}

	/** The major or minor key sharing this signature, e.g. C major ↔ A minor. */
	public KeySignature relative() {
		return new KeySignature(accidentals, !minor);
	}

	/** The signature after moving the music by {@code semitones}, spelled with the fewest accidentals. */
	public KeySignature transpose(int semitones) {
		// Each semitone up adds 7 sharps, modulo 12.
		int moved = Math.floorMod(accidentals + 7 * semitones, 12);
		return new KeySignature(moved > 6 ? moved - 12 : moved, minor);
	}

	public boolean flat() {
		return accidentals < 0;
	}

	public String name() {
		return (minor ? MINOR_NAMES : MAJOR_NAMES)[accidentals + 7] + (minor ? " minor" : " major");
	}
}
