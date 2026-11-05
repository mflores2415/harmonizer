package harmonizer.midi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/** Turns a sequence's NOTE_ON/NOTE_OFF events into {@link PlayedNote}s. */
public final class NoteReader {
	private NoteReader() {
	}

	/**
	 * Reads every note in the sequence, sorted by start tick then pitch. Repeated notes of the
	 * same pitch are paired first-in, first-out; notes never switched off end at their track's end.
	 */
	public static List<PlayedNote> read(Sequence sequence) {
		List<PlayedNote> notes = new ArrayList<>();
		Track[] tracks = sequence.getTracks();
		for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
			readTrack(trackIndex, tracks[trackIndex], notes);
		}
		notes.sort(Comparator.comparingLong(PlayedNote::startTick).thenComparingInt(PlayedNote::pitch));
		return List.copyOf(notes);
	}

	private static void readTrack(int trackIndex, Track track, List<PlayedNote> notes) {
		Map<Integer, Integer> programs = new HashMap<>();
		Map<Integer, Deque<Started>> sounding = new HashMap<>();
		for (int index = 0; index < track.size(); index++) {
			MidiEvent event = track.get(index);
			if (!(event.getMessage() instanceof ShortMessage message)) {
				continue;
			}
			int channel = message.getChannel();
			if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
				programs.put(channel, message.getData1());
			} else if (Midi.isNoteOn(message)) {
				sounding.computeIfAbsent(key(channel, message.getData1()), ignored -> new ArrayDeque<>())
						.addLast(new Started(programs.getOrDefault(channel, 0), event.getTick()));
			} else if (Midi.isNoteOff(message)) {
				Deque<Started> started = sounding.get(key(channel, message.getData1()));
				if (started != null && !started.isEmpty()) {
					Started note = started.removeFirst();
					notes.add(new PlayedNote(trackIndex, channel, note.program(), message.getData1(),
							note.tick(), event.getTick()));
				}
			}
		}
		long trackEnd = track.ticks();
		sounding.forEach((key, started) -> started.forEach(note -> notes.add(new PlayedNote(
				trackIndex, key / 128, note.program(), key % 128, note.tick(), trackEnd))));
	}

	private static int key(int channel, int pitch) {
		return channel * 128 + pitch;
	}

	private record Started(int program, long tick) {
	}
}
