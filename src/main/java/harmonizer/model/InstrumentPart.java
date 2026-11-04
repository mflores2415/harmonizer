package harmonizer.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
			"Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 ( chiff)", "Lead 5 (charang)",
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
	private final NavigableMap<Long, List<Integer>> notesByTick;

	private InstrumentPart(String name, NavigableMap<Long, List<Integer>> notesByTick) {
		this.name = name;
		this.notesByTick = notesByTick;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getNotesAt(long tick) {
		Map.Entry<Long, List<Integer>> entry = notesByTick.floorEntry(tick);
		return entry == null ? List.of() : entry.getValue();
	}

	public static String formatNote(int pitch) {
		return PITCH_NAMES[Math.floorMod(pitch, 12)] + (Math.floorDiv(pitch, 12) - 1);
	}

	static List<InstrumentPart> fromSequence(Sequence sequence) {
		List<InstrumentPart> parts = new ArrayList<>();
		Track[] tracks = sequence.getTracks();
		for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
			Track track = tracks[trackIndex];
			String trackName = readTrackName(track);
			Map<Integer, Integer> programs = new HashMap<>();
			Map<Integer, Deque<PartBuilder>> noteOwners = new HashMap<>();
			Map<PartKey, PartBuilder> builders = new LinkedHashMap<>();
			int eventIndex = 0;
			while (eventIndex < track.size()) {
				long tick = track.get(eventIndex).getTick();
				Set<PartBuilder> changedAtTick = new TreeSet<>((left, right) ->
						Integer.compare(left.order, right.order));
				while (eventIndex < track.size() && track.get(eventIndex).getTick() == tick) {
					MidiEvent event = track.get(eventIndex++);
					if (!(event.getMessage() instanceof ShortMessage message)) {
						continue;
					}
					int channel = message.getChannel();
					if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
						programs.put(channel, message.getData1());
						continue;
					}
					int command = message.getCommand();
					if (command != ShortMessage.NOTE_ON && command != ShortMessage.NOTE_OFF) {
						continue;
					}
					int pitch = message.getData1();
					int ownerKey = channel * 128 + pitch;
					if (command == ShortMessage.NOTE_ON && message.getData2() > 0) {
						int program = programs.getOrDefault(channel, 0);
						PartKey key = new PartKey(channel, program);
						PartBuilder builder = builders.computeIfAbsent(key,
								ignored -> new PartBuilder(parts.size() + builders.size(),
										buildPartName(trackName, channel, program)));
						noteOwners.computeIfAbsent(ownerKey, ignored -> new ArrayDeque<>()).addLast(builder);
						builder.changeNote(pitch, 1);
						changedAtTick.add(builder);
					} else {
						Deque<PartBuilder> owners = noteOwners.get(ownerKey);
						if (owners != null && !owners.isEmpty()) {
							PartBuilder builder = owners.removeFirst();
							builder.changeNote(pitch, -1);
							changedAtTick.add(builder);
							if (owners.isEmpty()) {
								noteOwners.remove(ownerKey);
							}
						}
					}
				}
				changedAtTick.forEach(builder -> builder.saveState(tick));
			}
			builders.values().stream()
					.filter(builder -> builder.hasNoteEvents)
					.forEach(builder -> parts.add(new InstrumentPart(builder.name, builder.notesByTick)));
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

	private record PartKey(int channel, int program) {
	}

	private static final class PartBuilder {
		private final int order;
		private final String name;
		private final Map<Integer, Integer> activeNotes = new HashMap<>();
		private final NavigableMap<Long, List<Integer>> notesByTick = new TreeMap<>();
		private boolean hasNoteEvents;

		private PartBuilder(int order, String name) {
			this.order = order;
			this.name = name;
		}

		private void changeNote(int pitch, int change) {
			hasNoteEvents = true;
			int count = activeNotes.getOrDefault(pitch, 0) + change;
			if (count <= 0) {
				activeNotes.remove(pitch);
			} else {
				activeNotes.put(pitch, count);
			}
		}

		private void saveState(long tick) {
			List<Integer> pitches = activeNotes.keySet().stream().sorted().toList();
			notesByTick.put(tick, pitches);
		}
	}
}
