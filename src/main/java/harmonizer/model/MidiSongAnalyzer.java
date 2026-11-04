package harmonizer.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

final class MidiSongAnalyzer {
	private static final int META_TEXT = 1;
	private static final int META_TRACK_NAME = 3;
	private static final int META_LYRIC = 5;
	private static final int META_TEMPO = 0x51;
	private static final int META_TIME_SIGNATURE = 0x58;
	private static final int META_KEY_SIGNATURE = 0x59;
	private static final String ANALYZED_MARKER = "Harm Analyzed";
	private static final Pattern CHORD_LABEL = Pattern.compile(
			"[A-G](?:#|b)?(?:maj|min|aug|dim|rr)(?:\\((?:m7|M7)\\))?");
	private static final String[] MAJOR_KEYS = {
			"Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#"
	};
	private static final String[] MINOR_KEYS = {
			"Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#"
	};

	private MidiSongAnalyzer() {
	}

	static Analysis analyze(Path midiFile, ChordAnalysisMode mode) throws IOException, InvalidMidiDataException {
		return analyze(MidiSystem.getSequence(midiFile.toFile()), mode, Map.of());
	}

	static Analysis analyze(
			Sequence sequence,
			ChordAnalysisMode mode,
			Map<Long, String> chordOverrides) throws InvalidMidiDataException {
		Track[] tracks = sequence.getTracks();
		if (tracks.length == 0) {
			throw new InvalidMidiDataException("MIDI sequence has no tracks.");
		}
		NavigableMap<Long, Chord> chords = new TreeMap<>();
		List<Long> chordTicks = new ArrayList<>();
		SongMetadata metadata = readMetadata(tracks);

		collectChords(tracks, chords, metadata.flatKey(), mode);
		chordOverrides.forEach((tick, name) -> {
			Chord chord = chords.get(tick);
			if (chord != null) {
				chords.put(tick, chord.withName(name));
			}
		});
		chordTicks.addAll(chords.navigableKeySet());

		Track lyricTrack = tracks[tracks.length - 1];
		if (metadata.analyzed()) {
			removeGeneratedLyrics(lyricTrack);
		} else {
			lyricTrack.add(new MidiEvent(createMetaMessage(META_TEXT, ANALYZED_MARKER), 0));
		}
		appendChordLyrics(chords, lyricTrack);

		return new Analysis(
				sequence,
				chords,
				chordTicks,
				metadata.name(),
				metadata.tempo(),
				metadata.timeSignature(),
				metadata.keySignature(),
				metadata.flatKey());
	}

	static Sequence copySequence(Sequence source) throws InvalidMidiDataException {
		Sequence copy = new Sequence(source.getDivisionType(), source.getResolution());
		for (Track sourceTrack : source.getTracks()) {
			Track targetTrack = copy.createTrack();
			for (int index = 0; index < sourceTrack.size(); index++) {
				MidiEvent event = sourceTrack.get(index);
				targetTrack.add(new MidiEvent((javax.sound.midi.MidiMessage) event.getMessage().clone(),
						event.getTick()));
			}
		}
		return copy;
	}

	private static SongMetadata readMetadata(Track[] tracks) {
		String name = null;
		String keySignature = "Unknown";
		String timeSignature = null;
		int tempo = 0;
		boolean flatKey = false;
		boolean analyzed = false;

		for (Track track : tracks) {
			for (int index = 0; index < track.size(); index++) {
				if (!(track.get(index).getMessage() instanceof MetaMessage message)) {
					continue;
				}

				byte[] data = message.getData();
				switch (message.getType()) {
					case META_TRACK_NAME -> name = new String(data, StandardCharsets.UTF_8);
					case META_TEXT -> analyzed |= ANALYZED_MARKER.equals(new String(data, StandardCharsets.UTF_8));
					case META_TEMPO -> tempo = parseTempo(data, tempo);
					case META_TIME_SIGNATURE -> timeSignature = parseTimeSignature(data, timeSignature);
					case META_KEY_SIGNATURE -> {
						if (data.length >= 2) {
							int accidentals = data[0];
							int mode = Byte.toUnsignedInt(data[1]);
							if (accidentals >= -7 && accidentals <= 7 && mode <= 1) {
								String[] keys = mode == 0 ? MAJOR_KEYS : MINOR_KEYS;
								keySignature = keys[accidentals + 7] + (mode == 0 ? " major" : " minor");
								flatKey = accidentals < 0;
							}
						}
					}
					default -> {
					}
				}
			}
		}

		return new SongMetadata(name, tempo, timeSignature, keySignature, flatKey, analyzed);
	}

	private static int parseTempo(byte[] data, int fallback) {
		if (data.length < 3) {
			return fallback;
		}
		int microsPerQuarterNote = (Byte.toUnsignedInt(data[0]) << 16)
				| (Byte.toUnsignedInt(data[1]) << 8)
				| Byte.toUnsignedInt(data[2]);
		return microsPerQuarterNote == 0
				? fallback
				: (int) Math.round(60_000_000.0 / microsPerQuarterNote);
	}

	private static String parseTimeSignature(byte[] data, String fallback) {
		if (data.length < 2) {
			return fallback;
		}
		int numerator = Byte.toUnsignedInt(data[0]);
		int denominatorExponent = Byte.toUnsignedInt(data[1]);
		if (denominatorExponent > 30) {
			return fallback;
		}
		return numerator + " / " + (1L << denominatorExponent);
	}

	private static void collectChords(
			Track[] tracks,
			NavigableMap<Long, Chord> chords,
			boolean flatKey,
			ChordAnalysisMode mode) {
		NavigableMap<Long, List<ShortMessage>> noteEvents = new TreeMap<>();
		for (Track track : tracks) {
			for (int index = 0; index < track.size(); index++) {
				MidiEvent event = track.get(index);
				if (!(event.getMessage() instanceof ShortMessage message) || message.getChannel() == 9) {
					continue;
				}
				if (message.getCommand() == ShortMessage.NOTE_ON || message.getCommand() == ShortMessage.NOTE_OFF) {
					noteEvents.computeIfAbsent(event.getTick(), tick -> new ArrayList<>()).add(message);
				}
			}
		}

		Map<Integer, Integer> activeNotes = new HashMap<>();
		Chord activeChord = null;
		for (Map.Entry<Long, List<ShortMessage>> entry : noteEvents.entrySet()) {
			Map<Integer, Integer> noteChanges = new HashMap<>();
			for (ShortMessage message : entry.getValue()) {
				if (isNoteOn(message)) {
					addNoteChange(noteChanges, message, 1);
				} else if (isNoteOff(message)) {
					addNoteChange(noteChanges, message, -1);
				}
			}
			applyNoteChanges(activeNotes, noteChanges);

			Chord chord = recognizeActiveChord(activeNotes, flatKey);
			if (chord == null) {
				activeChord = null;
			} else if (activeChord == null || !chord.equals(activeChord)
					&& (mode == ChordAnalysisMode.DETAILED
							|| chord.getRootPitchClass() != activeChord.getRootPitchClass())) {
				chords.put(entry.getKey(), chord);
				activeChord = chord;
			}
		}
	}

	private static boolean isNoteOn(ShortMessage message) {
		return message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0;
	}

	private static boolean isNoteOff(ShortMessage message) {
		return message.getCommand() == ShortMessage.NOTE_OFF
				|| (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0);
	}

	private static void addNoteChange(Map<Integer, Integer> noteChanges, ShortMessage message, int change) {
		int key = message.getChannel() * 128 + message.getData1();
		noteChanges.merge(key, change, Integer::sum);
	}

	private static void applyNoteChanges(
			Map<Integer, Integer> activeNotes,
			Map<Integer, Integer> noteChanges) {
		for (Map.Entry<Integer, Integer> entry : noteChanges.entrySet()) {
			int updatedCount = activeNotes.getOrDefault(entry.getKey(), 0) + entry.getValue();
			if (updatedCount <= 0) {
				activeNotes.remove(entry.getKey());
			} else {
				activeNotes.put(entry.getKey(), updatedCount);
			}
		}
	}

	private static Chord recognizeActiveChord(Map<Integer, Integer> activeNotes, boolean flatKey) {
		Set<Note> notes = new HashSet<>();
		for (int key : activeNotes.keySet()) {
			notes.add(new Note(key % 128));
		}
		if (notes.size() < 3) {
			return null;
		}
		Chord chord = new Chord(notes, flatKey);
		return "Err".equals(chord.getName()) ? null : chord;
	}

	private static void appendChordLyrics(
			NavigableMap<Long, Chord> chords,
			Track lyricTrack) throws InvalidMidiDataException {
		List<Map.Entry<Long, Chord>> entries = new ArrayList<>(chords.entrySet());
		for (int index = 0; index < entries.size(); index++) {
			Map.Entry<Long, Chord> entry = entries.get(index);
			String nextChord = index + 1 < entries.size()
					? entries.get(index + 1).getValue().getName()
					: "END";
			String lyric = entry.getValue().getName() + " " + nextChord;
			lyricTrack.add(new MidiEvent(createMetaMessage(META_LYRIC, lyric), entry.getKey()));
		}
	}

	private static void removeGeneratedLyrics(Track track) {
		for (int index = track.size() - 1; index >= 0; index--) {
			MidiEvent event = track.get(index);
			if (!(event.getMessage() instanceof MetaMessage message)
					|| message.getType() != META_LYRIC) {
				continue;
			}
			String[] lyricParts = new String(message.getData(), StandardCharsets.UTF_8).split(" ", 2);
			if (lyricParts.length == 2 && CHORD_LABEL.matcher(lyricParts[0]).matches()
					&& ("END".equals(lyricParts[1]) || CHORD_LABEL.matcher(lyricParts[1]).matches())) {
				track.remove(event);
			}
		}
	}

	private static MetaMessage createMetaMessage(int type, String value) throws InvalidMidiDataException {
		byte[] data = value.getBytes(StandardCharsets.UTF_8);
		MetaMessage message = new MetaMessage();
		message.setMessage(type, data, data.length);
		return message;
	}

	record Analysis(
			Sequence sequence,
			NavigableMap<Long, Chord> chords,
			List<Long> chordTicks,
			String name,
			int tempo,
			String timeSignature,
			String keySignature,
			boolean flatKey) {
	}

	private record SongMetadata(
			String name,
			int tempo,
			String timeSignature,
			String keySignature,
			boolean flatKey,
			boolean analyzed) {
	}
}
