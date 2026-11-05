package harmonizer.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public final class InstrumentPart {
	private static final int META_TRACK_NAME = 3;
	private static final String[] PROGRAM_NAMES = {
			"Acoustic Grand Piano", "Bright Piano", "Electric Grand Piano", "Honky-tonk Piano",
			"Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet",
			"Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba", "Xylophone",
			"Tubular Bells", "Dulcimer", "Drawbar Organ", "Percussive Organ", "Rock Organ",
			"Church Organ", "Reed Organ", "Accordion", "Harmonica", "Tango Accordion",
			"Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)",
			"Electric Guitar (clean)", "Electric Guitar (muted)", "Overdriven Guitar",
			"Distortion Guitar", "Guitar harmonics", "Acoustic Bass", "Electric Bass (finger)",
			"Electric Bass (pick)", "Fretless Bass", "Slap Bass 1", "Slap Bass 2",
			"Synth Bass 1", "Synth Bass 2", "Violin", "Viola", "Cello", "Contrabass",
			"Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani",
			"String Ensemble 1", "String Ensemble 2", "Synth Strings 1", "Synth Strings 2",
			"Choir Aahs", "Voice Oohs", "Synth Voice", "Orchestra Hit", "Trumpet", "Trombone",
			"Tuba", "Muted Trumpet", "French Horn", "Brass Section", "Synth Brass 1",
			"Synth Brass 2", "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax", "Oboe",
			"English Horn", "Bassoon", "Clarinet", "Piccolo", "Flute", "Recorder", "Pan Flute",
			"Blown Bottle", "Shakuhachi", "Whistle", "Ocarina", "Lead 1 (square)",
			"Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)", "Lead 5 (charang)",
			"Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass + lead)", "Pad 1 (new age)",
			"Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)", "Pad 5 (bowed)",
			"Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)", "FX 1 (rain)", "FX 2 (soundtrack)",
			"FX 3 (crystal)", "FX 4 (atmosphere)", "FX 5 (brightness)", "FX 6 (goblins)",
			"FX 7 (echoes)", "FX 8 (sci-fi)", "Sitar", "Banjo", "Shamisen", "Koto",
			"Kalimba", "Bag pipe", "Fiddle", "Shanai", "Tinkle Bell", "Agogo", "Steel Drums",
			"Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal",
			"Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet", "Telephone Ring",
			"Helicopter", "Applause", "Gunshot"
	};
	private static final String[] PITCH_NAMES = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};

	private final String name;
	private final List<NoteSpan> notes;
	private final int lowestPitch;
	private final int highestPitch;
	private final boolean percussion;

	private InstrumentPart(String name, List<NoteSpan> notes, boolean percussion) {
		this.name = name;
		this.percussion = percussion;
		this.notes = List.copyOf(notes);
		lowestPitch = notes.stream().mapToInt(NoteSpan::pitch).min().orElse(0);
		highestPitch = notes.stream().mapToInt(NoteSpan::pitch).max().orElse(0);
	}

	public String getName() {
		return name;
	}

	public List<NoteSpan> getNotes() {
		return notes;
	}

	public List<Integer> getNotesAt(long tick) {
		return notes.stream()
				.filter(note -> note.startTick() <= tick && tick < note.endTick())
				.map(NoteSpan::pitch)
				.distinct()
				.sorted()
				.toList();
	}

	public int getLowestPitch() {
		return lowestPitch;
	}

	public int getHighestPitch() {
		return highestPitch;
	}

	/** True for General MIDI channel 10, where pitches select drum sounds rather than notes. */
	public boolean isPercussion() {
		return percussion;
	}

	public static String formatNote(int pitch) {
		return PITCH_NAMES[Math.floorMod(pitch, 12)] + (Math.floorDiv(pitch, 12) - 1);
	}

	static List<InstrumentPart> fromSequence(Sequence sequence) {
		List<InstrumentPart> parts = new ArrayList<>();
		Track[] tracks = sequence.getTracks();
		for (Track track : tracks) {
			String trackName = readTrackName(track);
			Map<Integer, Integer> programs = new HashMap<>();
			Map<Integer, Deque<ActiveNote>> activeNotes = new HashMap<>();
			Map<PartKey, PartBuilder> builders = new LinkedHashMap<>();

			for (int index = 0; index < track.size(); index++) {
				MidiEvent event = track.get(index);
				if (!(event.getMessage() instanceof ShortMessage message)) {
					continue;
				}
				int channel = message.getChannel();
				if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
					programs.put(channel, message.getData1());
					continue;
				}
				if (message.getCommand() != ShortMessage.NOTE_ON
						&& message.getCommand() != ShortMessage.NOTE_OFF) {
					continue;
				}

				int pitch = message.getData1();
				int noteKey = channel * 128 + pitch;
				if (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0) {
					int program = programs.getOrDefault(channel, 0);
					PartKey key = new PartKey(channel, program);
					PartBuilder builder = builders.computeIfAbsent(key,
							ignored -> new PartBuilder(buildPartName(trackName, channel, program),
									channel == 9));
					activeNotes.computeIfAbsent(noteKey, ignored -> new ArrayDeque<>())
							.addLast(new ActiveNote(builder, pitch, event.getTick()));
					continue;
				}

				Deque<ActiveNote> notesForPitch = activeNotes.get(noteKey);
				if (notesForPitch == null || notesForPitch.isEmpty()) {
					continue;
				}
				ActiveNote active = notesForPitch.removeFirst();
				active.builder().notes.add(new NoteSpan(pitch, active.startTick(), event.getTick()));
				if (notesForPitch.isEmpty()) {
					activeNotes.remove(noteKey);
				}
			}

			long trackEnd = track.ticks();
			activeNotes.values().stream()
					.flatMap(Deque::stream)
					.forEach(active -> active.builder().notes.add(
							new NoteSpan(active.pitch(), active.startTick(), trackEnd)));
			builders.values().stream()
					.filter(builder -> !builder.notes.isEmpty())
					.forEach(builder -> {
						builder.notes.sort(Comparator.comparingLong(NoteSpan::startTick)
								.thenComparingInt(NoteSpan::pitch));
						parts.add(new InstrumentPart(builder.name, builder.notes, builder.percussion));
					});
		}
		return List.copyOf(parts);
	}

	private static String readTrackName(Track track) {
		for (int index = 0; index < track.size(); index++) {
			if (track.get(index).getMessage() instanceof MetaMessage message
					&& message.getType() == META_TRACK_NAME) {
				return new String(message.getData(), StandardCharsets.UTF_8).trim();
			}
		}
		return "";
	}

	private static String buildPartName(String trackName, int channel, int program) {
		String instrumentName = channel == 9 ? "Percussion" : PROGRAM_NAMES[program];
		String partName = trackName.isBlank() ? instrumentName : instrumentName + " — " + trackName;
		return partName + " (Ch. " + (channel + 1) + ")";
	}

	public record NoteSpan(int pitch, long startTick, long endTick) {
	}

	private record PartKey(int channel, int program) {
	}

	private record ActiveNote(PartBuilder builder, int pitch, long startTick) {
	}

	private static final class PartBuilder {
		private final String name;
		private final boolean percussion;
		private final List<NoteSpan> notes = new ArrayList<>();

		private PartBuilder(String name, boolean percussion) {
			this.name = name;
			this.percussion = percussion;
		}
	}
}
