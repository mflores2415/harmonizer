package harmonizer.player;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;

import harmonizer.midi.ChordLyrics;
import harmonizer.midi.ChordPair;
import harmonizer.midi.Midi;

/** Plays a sequence through the Java sound system and reports progress and the current chords. */
public final class MusicPlayer implements MusicPlayback, MetaEventListener {
	public static final double MIN_SPEED = 0.1;
	public static final double MAX_SPEED = 2.0;
	private static final long REWIND_INTERVAL_MICROSECONDS = 10_000_000L;

	private final Sequencer sequencer;
	private final NavigableMap<Long, ChordPair> chordLyrics = new TreeMap<>();
	private Sequence sequence;
	private double speed = 1.0;
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
		readChordLyrics(sequence);
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		sequencer.setTempoFactor((float) speed);
		sequencer.setMicrosecondPosition(0);
		notifyListener();
	}

	private void readChordLyrics(Sequence sequence) {
		chordLyrics.clear();
		for (Track track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				long tick = track.get(index).getTick();
				if (track.get(index).getMessage() instanceof MetaMessage message
						&& message.getType() == Midi.META_LYRIC) {
					ChordLyrics.parse(Midi.text(message)).ifPresent(chords -> chordLyrics.put(tick, chords));
				}
			}
		}
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
		Map.Entry<Long, ChordPair> lyric = chordLyrics.floorEntry(tick);
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
		sequencer.setMicrosecondPosition(Math.clamp(position, 0, getDurationMicroseconds()));
		notifyListener();
	}

	@Override
	public void setSpeed(double speed) {
		this.speed = Math.clamp(speed, MIN_SPEED, MAX_SPEED);
		sequencer.setTempoFactor((float) this.speed);
		notifyListener();
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	@Override
	public void close() {
		pause();
		sequencer.removeMetaEventListener(this);
		sequencer.close();
	}

	@Override
	public void meta(MetaMessage message) {
		if (message.getType() == Midi.META_LYRIC) {
			notifyListener();
		}
	}

	private void notifyListener() {
		listener.accept(getPlaybackUpdate());
	}
}
