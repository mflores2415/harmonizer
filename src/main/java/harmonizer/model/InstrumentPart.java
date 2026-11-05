package harmonizer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import harmonizer.midi.GeneralMidi;
import harmonizer.midi.Midi;
import harmonizer.midi.PlayedNote;

/** The notes one instrument plays: one channel and program within one track. */
public final class InstrumentPart {
	private final String name;
	private final List<PlayedNote> notes;
	private final int lowestPitch;
	private final int highestPitch;
	private final boolean percussion;

	private InstrumentPart(String name, List<PlayedNote> notes, boolean percussion) {
		this.name = name;
		this.notes = List.copyOf(notes);
		this.percussion = percussion;
		lowestPitch = notes.stream().mapToInt(PlayedNote::pitch).min().orElse(0);
		highestPitch = notes.stream().mapToInt(PlayedNote::pitch).max().orElse(0);
	}

	/**
	 * Splits a song's notes into instrument parts, ordered by track, then by when each part
	 * first plays, then by channel.
	 *
	 * @param notes      notes sorted by start tick, as {@link harmonizer.midi.NoteReader} returns them
	 * @param trackNames the name of each track, indexed like {@link PlayedNote#track()}
	 */
	static List<InstrumentPart> group(List<PlayedNote> notes, List<String> trackNames) {
		Map<PartKey, List<PlayedNote>> parts = new LinkedHashMap<>();
		for (PlayedNote note : notes) {
			parts.computeIfAbsent(PartKey.of(note), ignored -> new ArrayList<>()).add(note);
		}
		return parts.entrySet().stream()
				.sorted(Comparator.comparingInt((Map.Entry<PartKey, List<PlayedNote>> entry) -> entry.getKey().track())
						.thenComparingLong(entry -> entry.getValue().get(0).startTick())
						.thenComparingInt(entry -> entry.getKey().channel()))
				.map(entry -> {
					PartKey key = entry.getKey();
					String trackName = key.track() < trackNames.size() ? trackNames.get(key.track()) : "";
					return new InstrumentPart(key.displayName(trackName), entry.getValue(), key.percussion());
				})
				.toList();
	}

	public String getName() {
		return name;
	}

	/** The part's notes, sorted by start tick then pitch. */
	public List<PlayedNote> getNotes() {
		return notes;
	}

	/** The pitches sounding at {@code tick}, lowest first. */
	public List<Integer> getNotesAt(long tick) {
		return notes.stream()
				.filter(note -> note.soundsAt(tick))
				.map(PlayedNote::pitch)
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

	/** True for the drum channel, where pitches select drum sounds rather than notes. */
	public boolean isPercussion() {
		return percussion;
	}

	private record PartKey(int track, int channel, int program) {
		static PartKey of(PlayedNote note) {
			return new PartKey(note.track(), note.channel(), note.program());
		}

		boolean percussion() {
			return channel == Midi.DRUM_CHANNEL;
		}

		String displayName(String trackName) {
			String instrument = percussion() ? "Percussion" : GeneralMidi.programName(program);
			String name = trackName.isBlank() ? instrument : instrument + " — " + trackName;
			return name + " (Ch. " + (channel + 1) + ")";
		}
	}
}
