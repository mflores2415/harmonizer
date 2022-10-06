package edu.mccc.cos210.sp2016.ds.midi;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
public class MidiDrums extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private final PercussionInstrument drums[] = {
		new PercussionInstrument("Acoustic Bass Drum", 35),
		new PercussionInstrument("Bass Drum 1", 36),
		new PercussionInstrument("Side Stick", 37),
		new PercussionInstrument("Acoustic Snare", 38),
		new PercussionInstrument("Hand Clap", 39),
		new PercussionInstrument("Electric Snare", 40),
		new PercussionInstrument("Low Floor Tom", 41),
		new PercussionInstrument("Closed Hi-Hat", 42),
		new PercussionInstrument("High Floor Tom", 43),
		new PercussionInstrument("Pedal Hi-Hat", 44),
		new PercussionInstrument("Low Tom", 45),
		new PercussionInstrument("Open Hi-Hat", 46),
		new PercussionInstrument("Low-Mid Tom", 47),
		new PercussionInstrument("Hi-Mid Tom", 48),
		new PercussionInstrument("Crash Cymbal 1", 49),
		new PercussionInstrument("High Tom", 50),
		new PercussionInstrument("Ride Cymbal 1", 51),
		new PercussionInstrument("Chinese Cymbal", 52),
		new PercussionInstrument("Ride Bell", 53),
		new PercussionInstrument("Tambourine", 54),
		new PercussionInstrument("Splash Cymbal", 55),
		new PercussionInstrument("Cowbell", 56),
		new PercussionInstrument("Crash Cymbal 2", 57),
		new PercussionInstrument("Vibraslap", 58),
		new PercussionInstrument("Ride Cymbal 2", 59),
		new PercussionInstrument("Hi Bongo", 60),
		new PercussionInstrument("Low Bongo", 61),
		new PercussionInstrument("Mute Hi Conga", 62),
		new PercussionInstrument("Open Hi Conga", 63),
		new PercussionInstrument("Low Conga", 64),
		new PercussionInstrument("High Timbale", 65),
		new PercussionInstrument("Low Timbale", 66),
		new PercussionInstrument("High Agogo", 67),
		new PercussionInstrument("Low Agogo", 68),
		new PercussionInstrument("Cabasa", 69),
		new PercussionInstrument("Maracas", 70),
		new PercussionInstrument("Short Whistle", 71),
		new PercussionInstrument("Long Whistle", 72),
		new PercussionInstrument("Short Guiro", 73),
		new PercussionInstrument("Long Guiro", 74),
		new PercussionInstrument("Claves", 75),
		new PercussionInstrument("Hi Wood Block", 76),
		new PercussionInstrument("Low Wood Block", 77),
		new PercussionInstrument("Mute Cuica", 78),
		new PercussionInstrument("Open Cuica", 79),
		new PercussionInstrument("Mute Triangle", 80),
		new PercussionInstrument("Open Triangle", 81)
	};
	private final int PERCUSSION_CHANNEL = 9;
	private final int PERCUSSION_PATCH = 1;
//	Drumkit: Standard     bank #0 preset #0 patch 1
//	Drumkit: Room         bank #0 preset #8 patch 9
//	Drumkit: Power        bank #0 preset #16 patch 17
//	Drumkit: Electronic   bank #0 preset #24 patch 25
//	Drumkit: TR-808       bank #0 preset #25 patch 26
//	Drumkit: Jazz         bank #0 preset #32 patch 33
//	Drumkit: Brush        bank #0 preset #40 patch 41
//	Drumkit: Orchestra    bank #0 preset #48 patch 49
//	Drumkit: SFX          bank #0 preset #56 patch 57
	private final int DEFAULT_VELOCITY = 100;
	private final int PPQ = 8;
	@SuppressWarnings("unused")
	private final int BPMeasure = 4;
	private final int BPMinute = 120;
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;
	private int[][] song = {
		{ 00, 1,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 35, 1,0,0,0,0,0,0,0, 1,0,0,0,0,0,0,0, 1,0,0,0,0,0,0,0, 1,0,0,0,0,0,0,0 },
		{ 36, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 37, 1,0,0,0,0,0,1,0, 0,0,0,0,1,0,0,0, 1,0,0,0,1,0,0,0, 0,0,0,0,1,0,0,0 },
		{ 38, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 39, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 40, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 41, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 42, 1,0,1,0,1,0,1,0, 1,0,1,0,1,0,1,0, 1,0,1,0,1,0,1,0, 1,0,1,0,1,0,1,0 },
		{ 43, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 44, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 45, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 46, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 47, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 48, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 49, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 50, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 51, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 52, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 53, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 54, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 55, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 56, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 57, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 58, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 59, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 60, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 61, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 62, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 63, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 64, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 65, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 66, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 67, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 68, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 69, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 70, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 71, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 72, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 73, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 74, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 75, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 76, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 77, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 78, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 79, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 80, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 },
		{ 81, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 }
	};
	private MidiDrumControl mdc = new MidiDrumControl(drums, song);
	private JToggleButton jtb = new JToggleButton("Play");
	private FileDialog fd = new FileDialog(this, "Save As", FileDialog.SAVE);
	public MidiDrums() {
		super("MidiDrums");
		addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					sequencer.close();
					System.exit(0);
				}
			}
		);
		fd.setVisible(false);
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(1, 3));
		JButton jb = new JButton("Clear");
		jb.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					for (int i = 1; i < song.length; i++) {
						for (int j = 1; j < song[0].length; j++) {
							song[i][j] = 0;
						}
					}
					mdc.repaint();
				}
			}
		);
		jp.add(jb);
		jtb.addActionListener(this);
		jp.add(jtb);
		jb = new JButton("Save As...");
		jb.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						sequence = new Sequence(Sequence.PPQ, PPQ);
						track = sequence.createTrack();
						setPercussionChannel();
						for (int i = 0; i < song.length; i++) {
							for (int j = 1; j < song[0].length; j++) {
								if (song[i][j] != 0) {
									playIt(song[i][0], j - 1);
								}
							}
						}
						fd.setVisible(true);
						if (fd.getFile() != null) {
							MidiSystem.write(
								sequence,
								0,
								new File(
									fd.getDirectory(),
									fd.getFile()
								)
							);
						}
					} catch (Exception e) {
						System.err.println(e.getMessage());
						System.exit(-1);
					}
				}
			}
		);
		jp.add(jb);
		getContentPane().add(jp, BorderLayout.SOUTH);
		getContentPane().add(mdc, BorderLayout.CENTER);
		setSize(800, 575);
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	public static void main(String[] sa) {
		EventQueue.invokeLater(
			new Runnable() {
				@Override
				public void run() {
					new MidiDrums();
				}
			}
		);
	}
	private void createEvent(int type, int channel, int number, long tick) throws Exception {
		ShortMessage message = new ShortMessage();
		message.setMessage(type, channel, number, DEFAULT_VELOCITY);
		track.add(new MidiEvent(message, tick));
	}
	private void setPercussionChannel() throws Exception {
			createEvent(
				ShortMessage.PROGRAM_CHANGE,
				PERCUSSION_CHANNEL,
				PERCUSSION_PATCH,
				0
			);
	}
	private void playIt(int number, long tick) throws Exception {
		createEvent(
			ShortMessage.NOTE_ON,
			PERCUSSION_CHANNEL,
			number,
			tick
		);
		createEvent(
			ShortMessage.NOTE_OFF,
			PERCUSSION_CHANNEL,
			number,
			tick + 1
		);
	}
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof JToggleButton) {
			try {
				if (!jtb.isSelected()) {
					if (sequencer.isRunning()) {
						jtb.setText("Play");
						sequencer.stop();
					}
				} else {
					if (!sequencer.isRunning()) {
						jtb.setText("Stop");
						sequence = new Sequence(Sequence.PPQ, PPQ);
						track = sequence.createTrack();
						setPercussionChannel();
						playIt(0, 31);
						for (int i = 0; i < song.length; i++) {
							for (int j = 1; j < song[0].length; j++) {
								if (song[i][j] != 0) {
									playIt(song[i][0], j - 1);
								}
							}
						}
					}
					sequencer.setSequence(sequence);
					sequencer.setTempoInBPM(BPMinute);
					sequencer.setLoopStartPoint(0);
					sequencer.setLoopEndPoint(32);
					sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
					sequencer.start();
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}
	}
	public class PercussionInstrument {
		private String name;
		private int number;
		public PercussionInstrument(String s, int n) {
			name = s;
			number = n;
		}
		public String getName() {
			return name;
		}
		public int getNumber() {
			return number;
		}
	}
	private class MidiDrumControl extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;
		private PercussionInstrument[] mdpi;
		private int[][] song;
		private int textX = 8;
		private int gridX = 128;
		private int topY = 16;
		private int xSize = 16;
		private int ySize = 11;
		public MidiDrumControl(PercussionInstrument[] mdpi, int[][] song) {
			super();
			this.mdpi = mdpi;
			this.song = song;
			addMouseListener(this);
		}
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g.create();
			for (int i = 0; i < mdpi.length; i++) {
				g2d.drawString(mdpi[i].getName(), textX, topY + i * ySize);
			}
			for (int i = 1; i < song.length; i++) {
				for (int j = 1; j < song[0].length; j++) {
					Rectangle r = new Rectangle(xSize, ySize);
					r.translate(gridX + j * xSize, topY + (i - 2) * ySize);
					g2d.draw(r);
					if (song[i][j] != 0) {
						g2d.fill(r);
					}
				}
			}
			g2d.setStroke(new BasicStroke(2.0f));
			g2d.setPaint(Color.red);
			for (int i = 0; i < 5; i++) {
				g2d.drawLine(
					gridX + xSize + i * xSize * 8,
					topY - ySize + 1,
					gridX + xSize + i * xSize * 8,
					topY + 46 * ySize
				);
			}
		}
		public void mouseClicked(MouseEvent me) {
		}
		public void mouseEntered(MouseEvent me) {
		}
		public void mouseExited(MouseEvent me) {
		}
		public void mousePressed(MouseEvent me) {
		}
		public void mouseReleased(MouseEvent me) {
			int x = me.getX();
			int y = me.getY();
			int c = (x - gridX) / xSize;
			int r;
			if (y > topY) {
				r = (y - topY) / ySize + 2;
			} else {
				r = 1;
			}
			if (c > 0 && c < 32 && r > 0 && r < 48) {
				song[r][c] = song[r][c] == 1 ? 0 : 1;
			}
			repaint();
		}
	}
}
