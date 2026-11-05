package harmonizer.model;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import harmonizer.midi.Midi;

/** Moves a song's pitched notes up or down. */
final class Transposer {
	private Transposer() {
	}

	/**
	 * Returns a copy of {@code source} with every pitched note moved by {@code semitones}.
	 * Drums are left alone, notes pushed outside the MIDI range are dropped, and key signatures
	 * are rewritten so the displayed key and chord spelling follow along.
	 */
	static Sequence transpose(Sequence source, int semitones) throws InvalidMidiDataException {
		Sequence copy = Midi.copy(source);
		if (semitones == 0) {
			return copy;
		}
		for (Track track : copy.getTracks()) {
			for (int index = track.size() - 1; index >= 0; index--) {
				MidiEvent event = track.get(index);
				if (event.getMessage() instanceof ShortMessage message && isPitched(message)) {
					int pitch = message.getData1() + semitones;
					if (pitch < Midi.MIN_PITCH || pitch > Midi.MAX_PITCH) {
						track.remove(event);
					} else {
						message.setMessage(message.getCommand(), message.getChannel(), pitch, message.getData2());
					}
				} else if (event.getMessage() instanceof MetaMessage message) {
					var signature = KeySignature.from(message);
					if (signature.isPresent()) {
						byte[] data = signature.get().transpose(semitones).toData();
						message.setMessage(Midi.META_KEY_SIGNATURE, data, data.length);
					}
				}
			}
		}
		return copy;
	}

	private static boolean isPitched(ShortMessage message) {
		int command = message.getCommand();
		return !Midi.isDrum(message) && (command == ShortMessage.NOTE_ON
				|| command == ShortMessage.NOTE_OFF || command == ShortMessage.POLY_PRESSURE);
	}
}
