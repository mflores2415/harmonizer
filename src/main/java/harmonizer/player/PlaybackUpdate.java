package harmonizer.player;

import harmonizer.midi.ChordPair;

/**
 * A snapshot of playback.
 *
 * @param chords    the current and upcoming chords, or null before the first chord
 * @param chordTick tick where the current chord started (or the position, if there is none)
 * @param tick      the playback position in ticks
 */
public record PlaybackUpdate(
		long positionMicroseconds,
		long durationMicroseconds,
		boolean playing,
		ChordPair chords,
		long chordTick,
		long tick) {
}
