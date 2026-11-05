package harmonizer.midi;

/**
 * One note as it is played: a NOTE_ON paired with its NOTE_OFF.
 *
 * @param track   index of the track the note is in
 * @param channel MIDI channel (0–15)
 * @param program the instrument (General MIDI program) selected on the channel when the note began
 */
public record PlayedNote(int track, int channel, int program, int pitch, long startTick, long endTick) {
	public boolean isDrum() {
		return channel == Midi.DRUM_CHANNEL;
	}

	public long duration() {
		return endTick - startTick;
	}

	public boolean soundsAt(long tick) {
		return startTick <= tick && tick < endTick;
	}
}
