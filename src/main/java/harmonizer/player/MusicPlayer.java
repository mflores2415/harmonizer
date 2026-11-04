package harmonizer.player;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import harmonizer.ui.ChordDisplay;
import harmonizer.ui.Clock;

public class MusicPlayer implements IPlayable, MetaEventListener {
	private Sequencer sequencer = null;
	private Sequence sequence = null;
	private static final int META_Lyric = 5;
	private static final long REWIND_INTERVAL_MICROSECONDS = 10_000_000L;
	private final Clock clock;
	private final ChordDisplay cd;
	private final NavigableMap<Long, String> chordLyrics = new TreeMap<>();
	private JButton playPauseButton;
	private JButton rewindButton;
	private JButton restartButton;
	public MusicPlayer(Clock clock, ChordDisplay cd) {
		this.clock = clock;
		this.cd = cd;
		try {
			this.sequencer = MidiSystem.getSequencer(true);
			this.sequencer.addMetaEventListener(this);
		} catch (MidiUnavailableException e) {
			throw new IllegalStateException("No MIDI sequencer is available.", e);
		}
	}	
	public void setSequence(File file) throws Exception {
		this.setSequence(MidiSystem.getSequence(file));
	}	
	public void setSequence(Sequence s) throws Exception {
		pause();
		this.sequence = s;
		this.sequencer.setSequence(s);
		this.sequencer.setLoopCount(0);
		this.chordLyrics.clear();
		for (javax.sound.midi.Track track : s.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				if (track.get(index).getMessage() instanceof MetaMessage message
						&& message.getType() == META_Lyric) {
					chordLyrics.put(track.get(index).getTick(),
							new String(message.getData(), StandardCharsets.UTF_8));
				}
			}
		}
		if (!this.sequencer.isOpen()) {
			this.sequencer.open();
		}
		this.sequencer.setMicrosecondPosition(0);
		updateChordAtTick(0);
		updateControls(false);
	}
	public JPanel createPlayer() {
		playPauseButton = new JButton(new AbstractAction("Play") {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (sequencer != null && sequencer.isRunning()) {
					pause();
				} else {
					play();
				}
			}
		});
		playPauseButton.setName("playPauseButton");
		playPauseButton.setToolTipText("Play or pause the MIDI song");
		rewindButton = new JButton(new AbstractAction("<< 10s") {
			@Override
			public void actionPerformed(ActionEvent event) {
				rewind();
			}
		});
		restartButton = new JButton(new AbstractAction("Restart") {
			@Override
			public void actionPerformed(ActionEvent event) {
				replay();
			}
		});
		setControlsEnabled(false);

		JPanel controls = new JPanel();
		controls.add(rewindButton);
		controls.add(playPauseButton);
		controls.add(restartButton);
		return controls;
	}
	public long getDurationMicroseconds() {
		return sequence == null ? 0 : this.sequencer.getMicrosecondLength();
	}
	
	public long getPositionMicroseconds() {
		return sequence == null ? 0 : this.sequencer.getMicrosecondPosition();
	}

	public void play() {
		if (sequence == null) {
			return;
		}
		if (getPositionMicroseconds() >= getDurationMicroseconds()) {
			seekToMicroseconds(0);
		}
		this.sequencer.start();
		this.clock.play();
		updateControls(true);
	}	
	public void pause() {
		if (sequencer != null && sequencer.isRunning()) {
			this.sequencer.stop();
		}
		if (clock != null) {
			this.clock.pause();
		}
		updateControls(false);
	}	
	public void replay() {
		if (sequence != null) {
			seekToMicroseconds(0);
			clock.replay();
			play();
		}
	}
	
	public void rewind() {
		if (sequence != null) {
			seekToMicroseconds(Math.max(0, getPositionMicroseconds() - REWIND_INTERVAL_MICROSECONDS));
		}
	}

	public void seekToMicroseconds(long position) {
		if (sequence == null) {
			return;
		}
		long boundedPosition = Math.max(0, Math.min(position, getDurationMicroseconds()));
		sequencer.setMicrosecondPosition(boundedPosition);
		updateChordAtTick(sequencer.getTickPosition());
	}

	public void isStopped(boolean answer) {
		if (answer) {
			updateControls(false);
		}
	}

	public void close() {
		pause();
		if (sequencer != null) {
			sequencer.removeMetaEventListener(this);
			sequencer.close();
		}
	}

	private void updateChordAtTick(long tick) {
		Map.Entry<Long, String> lyric = chordLyrics.floorEntry(tick);
		if (lyric != null) {
			cd.set(lyric.getValue());
			cd.setTick(lyric.getKey());
		}
	}

	private void updateControls(boolean playing) {
		if (playPauseButton == null) {
			return;
		}
		Runnable update = () -> {
			playPauseButton.setEnabled(sequence != null);
			rewindButton.setEnabled(sequence != null);
			restartButton.setEnabled(sequence != null);
			playPauseButton.setText(playing ? "Pause" : "Play");
		};
		if (SwingUtilities.isEventDispatchThread()) {
			update.run();
		} else {
			SwingUtilities.invokeLater(update);
		}
	}

	private void setControlsEnabled(boolean enabled) {
		playPauseButton.setEnabled(enabled);
		rewindButton.setEnabled(enabled);
		restartButton.setEnabled(enabled);
	}

	@Override
	public void meta(MetaMessage meta) {
		if (meta.getType() == META_Lyric) {
			String lyric = new String(meta.getData(), StandardCharsets.UTF_8);
			cd.set(lyric);
			// Use the tick of the lyric event itself, not the playhead (which has
			// already moved past it), so the tick matches a key in Song's chord map.
			Map.Entry<Long, String> entry = chordLyrics.floorEntry(this.sequencer.getTickPosition());
			cd.setTick(entry != null ? entry.getKey() : this.sequencer.getTickPosition());
		}
	}
}
