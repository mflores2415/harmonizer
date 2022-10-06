package cos210.finalproject;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.MidiSystem;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Harmonizer {
	private Song song = null;
	private Clock clock;
	private MusicPlayer musicPlayer;
	@SuppressWarnings("unused")
	private UniqueChordWindow ucw;
	private FileInfoDisplay fileInfo;
	private ChordDisplay chordDisplay;
	private JPanel player;
	private JProgressBar progressBar;
	@SuppressWarnings("unused")
	private EditChordWindow ecw;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(Harmonizer::new);
	}
	@SuppressWarnings("serial")
	public Harmonizer() {
		JFrame jf = new JFrame("Harmonizer");
		JPanel jp = new JPanel();
		player = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
		jf.setPreferredSize(new Dimension(400,300));
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		createMenu(jf);	

		fileInfo = new FileInfoDisplay();
		chordDisplay = new ChordDisplay();
		clock = new Clock();
		musicPlayer = new MusicPlayer(clock, chordDisplay);
		
		clock.setMusicPlayer(musicPlayer);
		jp.add(chordDisplay.getDisplay());
		player.add(musicPlayer.createPlayer());
		progressBar = clock.getProgressBar();
		player.add(progressBar);
		
		JButton editButton = new JButton("Edit");
		player.add(editButton);
		jp.add(player);
		jp.add(fileInfo.get());
		
		jf.getContentPane().add(jp);
		jf.pack();
		jf.setResizable(false);
		jf.setLocationRelativeTo(null);
		jf.setVisible(true);
		
		
		editButton.setAction(new AbstractAction("Edit") {
			public void actionPerformed(ActionEvent ae) {
				showEditWindow();
			}
			});
		
	}
	
	
	public void showUniqueChords() {
		this.ucw = new UniqueChordWindow(this.song.getChords());		
	}

	public void showEditWindow() {
		this.musicPlayer.pause();
		this.ecw = new EditChordWindow(this.chordDisplay.getTick(), this.song, this.chordDisplay.getChords());
	}
	@SuppressWarnings("serial")
	public void createMenu(JFrame jf) {
		
		JMenuBar menuBar = new JMenuBar();
		jf.setJMenuBar(menuBar);
		
		
		JMenu fileMenu = new JMenu("File");
		JMenu viewMenu = new JMenu("View");
		JMenuItem importMenuItem = new JMenuItem("Import");
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		JMenuItem saveMenuItem = new JMenuItem("Save");
		JMenuItem showMenuItem = new JMenuItem("Show Unique Chords"); 
		saveMenuItem.setVisible(false);
		menuBar.add(fileMenu);
		fileMenu.add(importMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(exitMenuItem);
		menuBar.add(viewMenu);
		viewMenu.add(showMenuItem);
		exitMenuItem.setAction(new AbstractAction("Exit") {
			public void actionPerformed(ActionEvent sa) {
				System.exit(0);
			}
		});
		importMenuItem.setAction(new AbstractAction("Import") {
			public void actionPerformed(ActionEvent sa) {
				FileDialog fd = new FileDialog(jf, "Choose a MIDI file", FileDialog.LOAD);
				fd.setFile("*.mid");
				fd.setVisible(true);
				if(fd.getFile() != null) {
					try {
						loadSong(fd.getFile(), fd.getDirectory(), viewMenu, saveMenuItem);
						
					} catch (Exception e) {
					    JOptionPane.showMessageDialog(null, e.toString(), "Error",
                                JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			}
		});
		viewMenu.setEnabled(false);
		showMenuItem.setAction(new AbstractAction("Show All Chords") {
				public void actionPerformed(ActionEvent ae) {
					showUniqueChords();
				}
		});
		
		saveMenuItem.setAction(new AbstractAction("Save") {
			public void actionPerformed(ActionEvent sa) {
				FileDialog fd = new FileDialog(jf, "Save", FileDialog.SAVE);
				fd.setFile("*.mid");
				fd.setVisible(true);
				if(fd.getFile() != null) {
					try {
						saveSong(fd.getFile(), fd.getDirectory());
						
					} catch (Exception e) {
					    JOptionPane.showMessageDialog(null, e.toString(), "Error",
                                JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public void loadSong(String filename, String filedir, JMenuItem viewMenu, JMenuItem saveMenuItem) {
		try {
			this.song = new Song(filename, filedir, this.chordDisplay);
			this.musicPlayer.setSequence(this.song.getSequence());
			this.fileInfo.set(this.song);
			viewMenu.setEnabled(true);
			saveMenuItem.setVisible(true);
			this.clock.createClock();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void saveSong(String filename, String filedir) {
		try {
			MidiSystem.write(
				this.song.getSequence(),
				0,
				new File(
					filedir,
					filename
				)
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
