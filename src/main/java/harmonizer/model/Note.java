package harmonizer.model;

public class Note {
	private static final String[] PITCH_CLASS_NAMES = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};
	private final int pitch;
	private final int relativePitch;
	
	public Note(int pitch) {
		this.pitch = pitch;
		this.relativePitch = Math.floorMod(pitch, 12);
	}
	public String getName() {
		return PITCH_CLASS_NAMES[relativePitch];
	}
	public int getPitch() {
		return this.pitch;
	}
	
	public int getRelativePitch() {
		return this.relativePitch;
	}
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Note other && this.relativePitch == other.relativePitch;
	}

	@Override
	public int hashCode() {
		return this.relativePitch;
	}
 }
