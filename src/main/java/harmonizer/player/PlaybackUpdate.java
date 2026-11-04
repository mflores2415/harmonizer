package harmonizer.player;

public record PlaybackUpdate(
		long positionMicroseconds,
		long durationMicroseconds,
		boolean playing,
		String chordText,
		long chordTick,
		long tick) {
}
