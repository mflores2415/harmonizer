package harmonizer.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

/**
 * Harmonizer stores detected chords in the MIDI file as lyric events of the form
 * {@code "<current> <next>"} (e.g. {@code "Cmaj Gmaj"}, or {@code "Gmaj END"} for the last chord),
 * so saved files show chords in any karaoke player. This class owns that format.
 */
public final class ChordLyrics {
	public static final String END = "END";

	private static final String CHORD = "[A-G](?:#|b)?(?:maj|min|aug|dim|rr)(?:\\((?:m7|M7)\\))?";
	private static final Pattern LYRIC = Pattern.compile("(" + CHORD + ") (" + CHORD + "|" + END + ")");

	private ChordLyrics() {
	}

	public static String format(ChordPair pair) {
		return pair.current() + " " + pair.next();
	}

	/** Parses a lyric written by Harmonizer; other lyrics (e.g. karaoke words) give empty. */
	public static Optional<ChordPair> parse(String lyric) {
		var matcher = LYRIC.matcher(lyric);
		return matcher.matches() ? Optional.of(new ChordPair(matcher.group(1), matcher.group(2))) : Optional.empty();
	}

	/** Adds one chord lyric per chord change, each naming the chord that follows it. */
	public static void write(NavigableMap<Long, String> chordNames, Track track) throws InvalidMidiDataException {
		List<Map.Entry<Long, String>> entries = new ArrayList<>(chordNames.entrySet());
		for (int index = 0; index < entries.size(); index++) {
			String next = index + 1 < entries.size() ? entries.get(index + 1).getValue() : END;
			ChordPair pair = new ChordPair(entries.get(index).getValue(), next);
			track.add(new MidiEvent(Midi.textMessage(Midi.META_LYRIC, format(pair)), entries.get(index).getKey()));
		}
	}

	/** Removes chord lyrics from an earlier analysis, keeping any other lyrics. */
	public static void removeFrom(Track track) {
		for (int index = track.size() - 1; index >= 0; index--) {
			MidiEvent event = track.get(index);
			if (event.getMessage() instanceof MetaMessage message && message.getType() == Midi.META_LYRIC
					&& parse(Midi.text(message)).isPresent()) {
				track.remove(event);
			}
		}
	}
}
