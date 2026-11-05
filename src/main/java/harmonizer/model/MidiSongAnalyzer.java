package harmonizer.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import harmonizer.midi.ChordLyrics;
import harmonizer.midi.Midi;
import harmonizer.midi.NoteReader;
import harmonizer.midi.PlayedNote;

/**
 * Analyzes a song: reads its metadata and notes, settles on its key, detects its chords and
 * writes them into the sequence as chord lyrics for playback.
 */
final class MidiSongAnalyzer {
	private MidiSongAnalyzer() {
	}

	/**
	 * Analyzes {@code sequence}, adding chord lyrics to its last track. The sequence is changed,
	 * so callers pass a copy.
	 *
	 * @param chordNameCorrections chord names chosen by the user, keyed by chord tick
	 */
	static Analysis analyze(Sequence sequence, ChordAnalysisMode mode, Map<Long, String> chordNameCorrections)
			throws InvalidMidiDataException {
		Track[] tracks = sequence.getTracks();
		if (tracks.length == 0) {
			throw new InvalidMidiDataException("MIDI sequence has no tracks.");
		}
		SongMetadata metadata = SongMetadata.read(sequence);
		List<PlayedNote> notes = NoteReader.read(sequence);
		SongKey key = KeyResolver.resolve(notes, metadata.keySignature());

		NavigableMap<Long, Chord> chords = ChordDetector.detect(sequence, key.flat(), mode);
		chordNameCorrections.forEach((tick, name) -> chords.computeIfPresent(tick, (ignored, chord) -> chord.withName(name)));

		writeChordLyrics(tracks[tracks.length - 1], chords, metadata.analyzed());
		List<String> trackNames = Arrays.stream(tracks).map(Midi::trackName).toList();
		return new Analysis(sequence, chords, InstrumentPart.group(notes, trackNames),
				metadata.name(), metadata.tempo(), key);
	}

	private static void writeChordLyrics(Track track, NavigableMap<Long, Chord> chords, boolean previouslyAnalyzed)
			throws InvalidMidiDataException {
		if (previouslyAnalyzed) {
			ChordLyrics.removeFrom(track);
		} else {
			track.add(new MidiEvent(Midi.textMessage(Midi.META_TEXT, SongMetadata.ANALYZED_MARKER), 0));
		}
		NavigableMap<Long, String> names = new TreeMap<>();
		chords.forEach((tick, chord) -> names.put(tick, chord.getName()));
		ChordLyrics.write(names, track);
	}

	record Analysis(
			Sequence sequence,
			NavigableMap<Long, Chord> chords,
			List<InstrumentPart> instrumentParts,
			String name,
			int tempo,
			SongKey key) {
	}
}
