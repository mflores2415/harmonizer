package harmonizer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

/**
 * Maps MIDI ticks to bars and beats using the song's time signatures, including changes
 * mid-song. When a file has no time signature, 4/4 is assumed, as the MIDI standard specifies.
 */
public final class BarGrid {
	private static final int META_TIME_SIGNATURE = 0x58;

	/** A time signature taking effect at {@code tick}, which starts bar {@code firstBar}. */
	public record Section(long tick, int numerator, int denominator, int firstBar, long unitTicks) {
		/** Compound meters (6/8, 9/8, 12/8…) are felt in dotted-quarter beats of three units. */
		public boolean compound() {
			return denominator >= 8 && numerator > 3 && numerator % 3 == 0;
		}

		public long beatTicks() {
			return unitTicks * (compound() ? 3 : 1);
		}

		public long barTicks() {
			return unitTicks * numerator;
		}

		public String label() {
			return numerator + " / " + denominator;
		}
	}

	/** A grid line: either a bar start (with its 1-based bar number) or a beat inside a bar. */
	public record Line(long tick, boolean barStart, int bar) {
	}

	private final List<Section> sections;
	private final int resolution;
	private final boolean assumed;

	private BarGrid(List<Section> sections, int resolution, boolean assumed) {
		this.sections = List.copyOf(sections);
		this.resolution = resolution;
		this.assumed = assumed;
	}

	public static BarGrid fromSequence(Sequence sequence) {
		int resolution = Math.max(1, sequence.getResolution());
		TreeMap<Long, int[]> changes = new TreeMap<>();
		for (Track track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				MidiEvent event = track.get(index);
				if (event.getMessage() instanceof MetaMessage message
						&& message.getType() == META_TIME_SIGNATURE && message.getData().length >= 2) {
					int numerator = Byte.toUnsignedInt(message.getData()[0]);
					int exponent = Byte.toUnsignedInt(message.getData()[1]);
					if (numerator > 0 && exponent <= 6) {
						changes.put(event.getTick(), new int[] {numerator, 1 << exponent});
					}
				}
			}
		}
		boolean assumed = changes.isEmpty() || changes.firstKey() > 0;
		if (assumed) {
			changes.put(0L, new int[] {4, 4});
		}
		return of(changes, resolution, assumed && changes.size() == 1);
	}

	static BarGrid of(TreeMap<Long, int[]> changes, int resolution, boolean assumed) {
		List<Section> sections = new ArrayList<>();
		int bar = 1;
		Section previous = null;
		for (var change : changes.entrySet()) {
			int numerator = change.getValue()[0];
			int denominator = change.getValue()[1];
			if (previous != null) {
				if (previous.numerator() == numerator && previous.denominator() == denominator) {
					continue;
				}
				long elapsed = change.getKey() - previous.tick();
				bar += (int) Math.ceil(elapsed / (double) previous.barTicks());
			}
			long unitTicks = Math.max(1, resolution * 4L / denominator);
			previous = new Section(change.getKey(), numerator, denominator, bar, unitTicks);
			sections.add(previous);
		}
		return new BarGrid(sections, resolution, assumed);
	}

	public int getResolution() {
		return resolution;
	}

	public List<Section> getSections() {
		return sections;
	}

	/** True when the file had no time signature and 4/4 was assumed. */
	public boolean isAssumed() {
		return assumed;
	}

	/** The time signature in effect at {@code tick}. */
	public Section sectionAt(long tick) {
		Section found = sections.get(0);
		for (Section section : sections) {
			if (section.tick() > tick) {
				break;
			}
			found = section;
		}
		return found;
	}

	/** The 1-based bar number containing {@code tick}. */
	public int barAt(long tick) {
		Section section = sectionAt(Math.max(0, tick));
		return section.firstBar() + (int) ((Math.max(0, tick) - section.tick()) / section.barTicks());
	}

	/** Bar and beat lines between {@code fromTick} and {@code toTick}, inclusive. */
	public List<Line> lines(long fromTick, long toTick) {
		List<Line> lines = new ArrayList<>();
		for (int index = 0; index < sections.size(); index++) {
			Section section = sections.get(index);
			long sectionEnd = index + 1 < sections.size() ? sections.get(index + 1).tick() : Long.MAX_VALUE;
			if (sectionEnd <= fromTick || section.tick() > toTick) {
				continue;
			}
			long beat = section.beatTicks();
			long first = Math.max(0, (Math.max(fromTick, section.tick()) - section.tick()) / beat);
			for (long step = first; ; step++) {
				long tick = section.tick() + step * beat;
				if (tick > toTick || tick >= sectionEnd) {
					break;
				}
				long offset = tick - section.tick();
				boolean barStart = offset % section.barTicks() == 0;
				lines.add(new Line(tick, barStart, section.firstBar() + (int) (offset / section.barTicks())));
			}
		}
		return lines;
	}

	/** Short description for the song details, e.g. "6 / 8" or "4 / 4 (changes)". */
	public String describe() {
		String first = sections.get(0).label();
		if (assumed) {
			return first + " (assumed)";
		}
		return sections.size() > 1 ? first + " (changes)" : first;
	}

	/** Every time signature with the bar it starts at, for a tooltip. */
	public String describeChanges() {
		StringBuilder text = new StringBuilder();
		for (Section section : sections) {
			if (!text.isEmpty()) {
				text.append('\n');
			}
			text.append("Bar ").append(section.firstBar()).append(": ").append(section.label());
		}
		return text.toString();
	}
}
