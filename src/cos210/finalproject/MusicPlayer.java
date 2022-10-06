package cos210.finalproject;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;



import cos210.finalproject.iface.IPlayable;

public class MusicPlayer implements IPlayable, MetaEventListener {
	private Sequencer sequencer = null;
	private Sequence sequence = null;
	private Synthesizer synth = null;
	private static final int META_EndofTrack = 47;
	private static final int META_Lyric = 5;
	private Clock clock;	
	private JButton playButton;
	private JButton pauseButton;
	private JButton replayButton;
	private long currentTick = 0;
	private ChordDisplay cd = null;
	protected boolean isStopped;
	public MusicPlayer(Clock clock, ChordDisplay cd) {
		this.clock = clock;
		this.cd = cd;
		try {
			this.sequencer = MidiSystem.getSequencer(true);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}	
	public void setSequence(File file) throws Exception {
		this.setSequence(MidiSystem.getSequence(file));
	}	
	public void setSequence(Sequence s) throws Exception {
		this.currentTick = 0;
		this.sequencer.setTickPosition(this.currentTick);
		this.sequence = s;
		this.sequencer.setSequence(s);
		this.sequencer.setLoopCount(0);
		this.sequencer.addMetaEventListener(this);
		this.synth = MidiSystem.getSynthesizer();
		this.synth.open();
		this.sequencer.open();
	}
	@SuppressWarnings("serial")
	public JPanel createPlayer() {
		ImageIcon play = new ImageIcon(".\\images\\play.png");
		playButton = new JButton(play);
		playButton.setVisible(true);
		playButton.setEnabled(false);
		playButton.setAction(new AbstractAction("", play) {
			public void actionPerformed(ActionEvent sa) {
				if (sequencer != null) {
					play();	
				}			
			}
		});
		ImageIcon pause = new ImageIcon(".\\images\\pause.png");
		pauseButton = new JButton("pause");
		pauseButton.setVisible(false);
		pauseButton.setAction(new AbstractAction("", pause) {
			public void actionPerformed(ActionEvent sa) {
				if (sequencer != null) {
					pause();	
				}
			}
		});
		ImageIcon replay = new ImageIcon(".\\images\\replay.png");
		replayButton = new JButton("replay");
		replayButton.setVisible(false);
		replayButton.setAction(new AbstractAction("", replay) {
			public void actionPerformed(ActionEvent sa) {
				if (sequencer != null) {
					replay();	
				}
			}
		});		
		JPanel clockdisplay = new JPanel();
		clockdisplay.add(playButton);
		clockdisplay.add(pauseButton);
		clockdisplay.add(replayButton);		
		return clockdisplay;
	}
	public long getMSLength() {
		return this.sequencer.getMicrosecondLength();
	}
	
	public long getMSPosition() {
		return this.sequencer.getMicrosecondPosition();
	}

	public void play() {
		this.sequencer.setTickPosition(this.currentTick);
		this.sequencer.start();
		this.clock.play();
		playButton.setVisible(false);
		pauseButton.setVisible(true);
	}	
	public void pause() {
		this.currentTick = sequencer.getTickPosition();
		this.sequencer.stop();
		this.clock.pause();
		pauseButton.setVisible(false);
		playButton.setVisible(true);
	}	
	public void replay() {
		this.currentTick = 0;
		this.sequencer.setTickPosition(this.currentTick);
		try {
			this.setSequence(this.sequence);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.sequencer.start();
		this.clock.replay();
		replayButton.setVisible(false);
		pauseButton.setVisible(true);
	}
	
	public void isStopped(boolean answer) {
		if (answer) {
			playButton.setVisible(false);
			pauseButton.setVisible(false);
			replayButton.setVisible(true);
		}
	}
	@Override
	public void meta(MetaMessage meta) {
		if (meta.getType() == META_EndofTrack) {
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			} finally {
				this.synth.close();
				this.sequencer.close();
			}
		} else {
			if (meta.getType() == META_Lyric) {
				try {
					cd.set(new String(meta.getData(), "UTF-8"));
					cd.setTick(this.sequencer.getTickPosition());
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
	}
}
