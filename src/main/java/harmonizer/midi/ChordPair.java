package harmonizer.midi;

import java.util.Objects;

/**
 * The chord playing now and the one coming up next, as carried by Harmonizer's chord lyrics.
 *
 * @param next the upcoming chord, or {@link ChordLyrics#END} after the last chord
 */
public record ChordPair(String current, String next) {
	public ChordPair {
		Objects.requireNonNull(current, "current");
		Objects.requireNonNull(next, "next");
	}
}
