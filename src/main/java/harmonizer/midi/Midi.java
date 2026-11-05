package harmonizer.midi;

import java.nio.charset.StandardCharsets;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/** Constants and small helpers for working with {@code javax.sound.midi}. */
public final class Midi {
	public static final int META_TEXT = 0x01;
	public static final int META_TRACK_NAME = 0x03;
	public static final int META_LYRIC = 0x05;
	public static final int META_TEMPO = 0x51;
	public static final int META_TIME_SIGNATURE = 0x58;
	public static final int META_KEY_SIGNATURE = 0x59;

	/** General MIDI reserves channel 10 (index 9) for drums. */
	public static final int DRUM_CHANNEL = 9;
	public static final int MIN_PITCH = 0;
	public static final int MAX_PITCH = 127;
	/** Tempo used when a file has no tempo event, per the MIDI standard. */
	public static final int DEFAULT_TEMPO_BPM = 120;

	private Midi() {
	}

	public static boolean isNoteOn(ShortMessage message) {
		return message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0;
	}

	/** A NOTE_OFF, or a NOTE_ON with velocity 0 (which MIDI treats as a note off). */
	public static boolean isNoteOff(ShortMessage message) {
		return message.getCommand() == ShortMessage.NOTE_OFF
				|| message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0;
	}

	public static boolean isDrum(ShortMessage message) {
		return message.getChannel() == DRUM_CHANNEL;
	}

	public static String text(MetaMessage message) {
		return new String(message.getData(), StandardCharsets.UTF_8);
	}

	public static MetaMessage textMessage(int type, String text) throws InvalidMidiDataException {
		byte[] data = text.getBytes(StandardCharsets.UTF_8);
		MetaMessage message = new MetaMessage();
		message.setMessage(type, data, data.length);
		return message;
	}

	/** The track's name from its first track-name event, or an empty string. */
	public static String trackName(Track track) {
		for (int index = 0; index < track.size(); index++) {
			if (track.get(index).getMessage() instanceof MetaMessage message
					&& message.getType() == META_TRACK_NAME) {
				return text(message).trim();
			}
		}
		return "";
	}

	/** A deep copy, so the copy's events can be changed without affecting {@code source}. */
	public static Sequence copy(Sequence source) throws InvalidMidiDataException {
		Sequence copy = new Sequence(source.getDivisionType(), source.getResolution());
		for (Track sourceTrack : source.getTracks()) {
			Track targetTrack = copy.createTrack();
			for (int index = 0; index < sourceTrack.size(); index++) {
				MidiEvent event = sourceTrack.get(index);
				targetTrack.add(new MidiEvent((MidiMessage) event.getMessage().clone(), event.getTick()));
			}
		}
		return copy;
	}
}
