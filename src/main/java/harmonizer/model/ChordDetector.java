package harmonizer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import harmonizer.midi.Midi;

/**
 * Finds the chord changes in a song: at every tick where notes start or stop, the pitched notes
 * sounding across all tracks are matched against known chords.
 */
final class ChordDetector {
	private static final int MIN_CHORD_NOTES = 3;

	private ChordDetector() {
	}

	/** Chords keyed by the tick where each one starts. */
	static NavigableMap<Long, Chord> detect(Sequence sequence, boolean flatKey, ChordAnalysisMode mode) {
		NavigableMap<Long, Chord> chords = new TreeMap<>();
		Map<Integer, Integer> sounding = new HashMap<>();
		Chord activeChord = null;
		for (var entry : noteEventsByTick(sequence).entrySet()) {
			apply(entry.getValue(), sounding);
			Optional<Chord> chord = recognize(sounding.keySet(), flatKey);
			if (chord.isEmpty()) {
				activeChord = null;
			} else if (startsNewChord(chord.get(), activeChord, mode)) {
				chords.put(entry.getKey(), chord.get());
				activeChord = chord.get();
			}
		}
		return chords;
	}

	/**
	 * Detailed mode reports every change of chord; steady mode only reports a change of root,
	 * keeping the first quality while the root stays the same.
	 */
	private static boolean startsNewChord(Chord chord, Chord activeChord, ChordAnalysisMode mode) {
		if (activeChord == null) {
			return true;
		}
		if (chord.equals(activeChord)) {
			return false;
		}
		return mode == ChordAnalysisMode.DETAILED || chord.getRootPitchClass() != activeChord.getRootPitchClass();
	}

	private static NavigableMap<Long, List<ShortMessage>> noteEventsByTick(Sequence sequence) {
		NavigableMap<Long, List<ShortMessage>> events = new TreeMap<>();
		for (Track track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				MidiEvent event = track.get(index);
				if (event.getMessage() instanceof ShortMessage message && !Midi.isDrum(message)
						&& (Midi.isNoteOn(message) || Midi.isNoteOff(message))) {
					events.computeIfAbsent(event.getTick(), tick -> new ArrayList<>()).add(message);
				}
			}
		}
		return events;
	}

	/** Updates how many times each channel+pitch is sounding after one tick's events. */
	private static void apply(List<ShortMessage> events, Map<Integer, Integer> sounding) {
		Map<Integer, Integer> changes = new HashMap<>();
		for (ShortMessage message : events) {
			int key = message.getChannel() * 128 + message.getData1();
			changes.merge(key, Midi.isNoteOn(message) ? 1 : -1, Integer::sum);
		}
		changes.forEach((key, change) -> {
			int count = sounding.getOrDefault(key, 0) + change;
			if (count <= 0) {
				sounding.remove(key);
			} else {
				sounding.put(key, count);
			}
		});
	}

	private static Optional<Chord> recognize(Set<Integer> soundingKeys, boolean flatKey) {
		Set<Note> notes = new HashSet<>();
		for (int key : soundingKeys) {
			notes.add(new Note(key % 128));
		}
		if (notes.size() < MIN_CHORD_NOTES) {
			return Optional.empty();
		}
		Chord chord = new Chord(notes, flatKey);
		return chord.isRecognized() ? Optional.of(chord) : Optional.empty();
	}
}
