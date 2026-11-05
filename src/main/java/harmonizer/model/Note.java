package harmonizer.model;

/**
 * A note reduced to its pitch class: two notes are equal when they have the same name,
 * whatever their octave, which is what chord recognition needs.
 */
public final class Note {
	private final int pitch;
	private final int relativePitch;

	public Note(int pitch) {
		this.pitch = pitch;
		this.relativePitch = Pitch.pitchClass(pitch);
	}

	public String getName() {
		return Pitch.name(relativePitch, false);
	}

	public int getPitch() {
		return pitch;
	}

	public int getRelativePitch() {
		return relativePitch;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Note other && relativePitch == other.relativePitch;
	}

	@Override
	public int hashCode() {
		return relativePitch;
	}
}
