package harmonizer.model;

import java.util.Objects;
import java.util.Set;

public final class Chord {
	private static final String[] SHARP_ROOTS = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};
	private static final String[] FLAT_ROOTS = {
			"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
	};
	private static final ChordQuality[] QUALITIES = {
			new ChordQuality("maj", new int[] { 0, 4, 7 }),
			new ChordQuality("min", new int[] { 0, 3, 7 }),
			new ChordQuality("maj(m7)", new int[] { 0, 4, 7, 10 }),
			new ChordQuality("maj(M7)", new int[] { 0, 4, 7, 11 }),
			new ChordQuality("min(m7)", new int[] { 0, 3, 7, 10 }),
			new ChordQuality("min(M7)", new int[] { 0, 3, 7, 11 }),
			new ChordQuality("aug", new int[] { 0, 4, 8 }),
			new ChordQuality("dim", new int[] { 0, 3, 6 })
	};
	private static final int MAX_EXTRA_NOTES = 2;

	private final String name;
	private final String type;
	private final int root;
	private final Set<Note> notes;
	private final boolean flatKey;

	public Chord(Set<Note> notes, boolean flatKey) {
		this.notes = Set.copyOf(notes);
		this.flatKey = flatKey;
		ChordMatch match = recognize(pitchMask(this.notes));
		root = match == null ? 4 : match.root();
		type = match == null ? "rr" : match.quality().name();
		String[] roots = flatKey ? FLAT_ROOTS : SHARP_ROOTS;
		name = roots[root] + type;
	}

	private Chord(Chord chord, String name) {
		notes = chord.notes;
		flatKey = chord.flatKey;
		root = chord.root;
		type = chord.type;
		this.name = name;
	}

	public Chord withName(String name) {
		return new Chord(this, name);
	}

	private static int pitchMask(Set<Note> notes) {
		int mask = 0;
		for (Note note : notes) {
			mask |= 1 << note.getRelativePitch();
		}
		return mask;
	}

	private static ChordMatch recognize(int pitchMask) {
		ChordMatch bestMatch = null;
		for (int root = 0; root < 12; root++) {
			for (ChordQuality quality : QUALITIES) {
				int requiredMask = quality.pitchMask(root);
				int matchedNotes = Integer.bitCount(pitchMask & requiredMask);
				if (matchedNotes < 3) {
					continue;
				}

				int extraNotes = Integer.bitCount(pitchMask & ~requiredMask);
				if (extraNotes > MAX_EXTRA_NOTES) {
					continue;
				}
				int matchedQualityBonus = matchedNotes == quality.intervals().length
						? 2 + quality.intervals().length - 3
						: -2 * (quality.intervals().length - matchedNotes);
				int score = matchedNotes * 10 - extraNotes * 4 + matchedQualityBonus;
				if (bestMatch == null || score > bestMatch.score()) {
					bestMatch = new ChordMatch(root, quality, score);
				}
			}
		}
		return bestMatch;
	}

	public String getName() {
		return name;
	}

	public int getRootPitchClass() {
		return root;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof Chord other && Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	private record ChordQuality(String name, int[] intervals) {
		int pitchMask(int root) {
			int mask = 0;
			for (int interval : intervals) {
				mask |= 1 << ((root + interval) % 12);
			}
			return mask;
		}
	}

	private record ChordMatch(int root, ChordQuality quality, int score) {
	}
}
