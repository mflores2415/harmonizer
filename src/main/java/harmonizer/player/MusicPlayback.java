package harmonizer.player;

import java.util.function.Consumer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

public interface MusicPlayback extends AutoCloseable {
	void setListener(Consumer<PlaybackUpdate> listener);

	void setSequence(Sequence sequence) throws InvalidMidiDataException, MidiUnavailableException;

	long getDurationMicroseconds();

	long getPositionMicroseconds();

	boolean isPlaying();

	PlaybackUpdate getPlaybackUpdate();

	void play();

	void pause();

	void replay();

	void rewind();

	void seekToMicroseconds(long position);

	@Override
	void close();
}
