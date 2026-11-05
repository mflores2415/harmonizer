package harmonizer.model;

import java.util.Optional;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import harmonizer.midi.Midi;

/**
 * Information stored in a MIDI file's meta events.
 *
 * @param name         the song's name (its last track-name event), or null
 * @param tempo        tempo in BPM, or 0 if the file doesn't say
 * @param keySignature the file's key signature, if any
 * @param analyzed     true if Harmonizer has already added chord lyrics to this file
 */
record SongMetadata(String name, int tempo, Optional<KeySignature> keySignature, boolean analyzed) {
	/** Text event Harmonizer adds so it can recognize files it has saved before. */
	static final String ANALYZED_MARKER = "Harm Analyzed";

	static SongMetadata read(Sequence sequence) {
		String name = null;
		int tempo = 0;
		Optional<KeySignature> keySignature = Optional.empty();
		boolean analyzed = false;
		for (Track track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				if (!(track.get(index).getMessage() instanceof MetaMessage message)) {
					continue;
				}
				switch (message.getType()) {
					case Midi.META_TRACK_NAME -> name = Midi.text(message);
					case Midi.META_TEXT -> analyzed |= ANALYZED_MARKER.equals(Midi.text(message));
					case Midi.META_TEMPO -> tempo = parseTempo(message.getData(), tempo);
					case Midi.META_KEY_SIGNATURE -> {
						Optional<KeySignature> parsed = KeySignature.from(message);
						if (parsed.isPresent()) {
							keySignature = parsed;
						}
					}
					default -> {
					}
				}
			}
		}
		return new SongMetadata(name, tempo, keySignature, analyzed);
	}

	private static int parseTempo(byte[] data, int fallback) {
		if (data.length < 3) {
			return fallback;
		}
		int microsPerQuarterNote = (Byte.toUnsignedInt(data[0]) << 16)
				| (Byte.toUnsignedInt(data[1]) << 8)
				| Byte.toUnsignedInt(data[2]);
		return microsPerQuarterNote == 0 ? fallback : (int) Math.round(60_000_000.0 / microsPerQuarterNote);
	}
}
