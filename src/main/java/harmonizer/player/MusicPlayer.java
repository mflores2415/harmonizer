package harmonizer.player;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;

public final class MusicPlayer implements MusicPlayback, MetaEventListener {
	private static final int META_LYRIC = 5;
	private static final long REWIND_INTERVAL_MICROSECONDS = 10_000_000L;
	private static final Pattern CHORD_LYRIC = Pattern.compile(
			"[A-G](?:#|b)?(?:maj|min|aug|dim|rr)(?:\\((?:m7|M7)\\))? "
					+ "(?:[A-G](?:#|b)?(?:maj|min|aug|dim|rr)(?:\\((?:m7|M7)\\))?|END)");

	private final Sequencer sequencer;
	private final NavigableMap<Long, String> chordLyrics = new TreeMap<>();
	private Sequence sequence;
	private Consumer<PlaybackUpdate> listener = update -> { };

	public MusicPlayer() {
		try {
			sequencer = MidiSystem.getSequencer(true);
			sequencer.addMetaEventListener(this);
		} catch (MidiUnavailableException exception) {
			throw new IllegalStateException("No MIDI sequencer is available.", exception);
		}
	}

	@Override
	public void setListener(Consumer<PlaybackUpdate> listener) {
		this.listener = listener == null ? update -> { } : listener;
		notifyListener();
	}

	@Override
	public void setSequence(Sequence sequence) throws InvalidMidiDataException, MidiUnavailableException {
		pause();
		this.sequence = sequence;
		sequencer.setSequence(sequence);
		sequencer.setLoopCount(0);
		chordLyrics.clear();
		for (Track track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				if (track.get(index).getMessage() instanceof MetaMessage message
						&& message.getType() == META_LYRIC) {
					String lyric = new String(message.getData(), StandardCharsets.UTF_8);
					if (CHORD_LYRIC.matcher(lyric).matches()) {
						chordLyrics.put(track.get(index).getTick(), lyric);
					}
				}
			}
		}
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		sequencer.setMicrosecondPosition(0);
		notifyListener();
	}

	@Override
	public long getDurationMicroseconds() {
		return sequence == null ? 0 : sequencer.getMicrosecondLength();
	}

	@Override
	public long getPositionMicroseconds() {
		return sequence == null ? 0 : sequencer.getMicrosecondPosition();
	}

	@Override
	public boolean isPlaying() {
		return sequence != null && sequencer.isRunning();
	}

	@Override
	public PlaybackUpdate getPlaybackUpdate() {
		long tick = sequence == null ? 0 : sequencer.getTickPosition();
		Map.Entry<Long, String> lyric = chordLyrics.floorEntry(tick);
		return new PlaybackUpdate(
				getPositionMicroseconds(),
				getDurationMicroseconds(),
				isPlaying(),
				lyric == null ? null : lyric.getValue(),
				lyric == null ? tick : lyric.getKey(),
				tick);
	}

	@Override
	public void play() {
		if (sequence == null) {
			return;
		}
		if (getPositionMicroseconds() >= getDurationMicroseconds()) {
			seekToMicroseconds(0);
		}
		sequencer.start();
		notifyListener();
	}

	@Override
	public void pause() {
		if (sequencer.isRunning()) {
			sequencer.stop();
		}
		notifyListener();
	}

	@Override
	public void replay() {
		if (sequence != null) {
			seekToMicroseconds(0);
			play();
		}
	}

	@Override
	public void rewind() {
		if (sequence != null) {
			seekToMicroseconds(Math.max(0, getPositionMicroseconds() - REWIND_INTERVAL_MICROSECONDS));
		}
	}

	@Override
	public void seekToMicroseconds(long position) {
		if (sequence == null) {
			return;
		}
		long boundedPosition = Math.max(0, Math.min(position, getDurationMicroseconds()));
		sequencer.setMicrosecondPosition(boundedPosition);
		notifyListener();
	}

	@Override
	public void close() {
		pause();
		sequencer.removeMetaEventListener(this);
		sequencer.close();
	}

	@Override
	public void meta(MetaMessage message) {
		if (message.getType() == META_LYRIC) {
			notifyListener();
		}
	}

	private void notifyListener() {
		listener.accept(getPlaybackUpdate());
	}
}
