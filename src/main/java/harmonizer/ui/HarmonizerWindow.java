package harmonizer.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;

import harmonizer.model.Song;
import harmonizer.player.MusicPlayer;

public final class HarmonizerWindow {
	public interface Actions {
		void importMidi(File file);
		void saveMidi(File file);
		void showUniqueChords();
		void editChord();
		void closePlayer();
	}

	private final JFrame frame;
	private final FileInfoDisplay fileInfo;
	private final CardLayout songViewLayout;
	private final JPanel songView;
	private final JPanel emptyState;
	private final JButton editButton;
	private final JLabel songTitle;
	private JMenuItem saveMenuItem;
	private JMenu viewMenu;
	private final Actions actions;

	public HarmonizerWindow(ChordDisplay chordDisplay, MusicPlayer musicPlayer, Clock clock, Actions actions) {
		this.actions = actions;
		frame = new JFrame("Harmonizer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				actions.closePlayer();
			}
		});
		frame.setMinimumSize(new Dimension(680, 480));
		frame.setPreferredSize(new Dimension(820, 600));
		frame.setJMenuBar(createMenuBar());

		fileInfo = new FileInfoDisplay();
		JPanel content = new JPanel(new BorderLayout(12, 16));
		content.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

		JPanel header = new JPanel(new BorderLayout(12, 0));
		JLabel appTitle = new JLabel("Harmonizer");
		appTitle.setFont(appTitle.getFont().deriveFont(java.awt.Font.BOLD, 22f));
		songTitle = new JLabel("Listen closer. See the harmony.", SwingConstants.TRAILING);
		songTitle.setForeground(new java.awt.Color(90, 99, 112));
		JButton openMidiButton = new JButton("Open MIDI...");
		openMidiButton.addActionListener(event -> chooseMidiFile(false));
		header.add(appTitle, BorderLayout.WEST);
		header.add(songTitle, BorderLayout.CENTER);
		header.add(openMidiButton, BorderLayout.EAST);

		songViewLayout = new CardLayout();
		songView = new JPanel(songViewLayout);
		emptyState = createEmptyState();
		songView.add(emptyState, "empty");
		songView.add(chordDisplay.getDisplay(), "song");
		songViewLayout.show(songView, "empty");

		JPanel playbackPanel = new JPanel(new BorderLayout(8, 8));
		playbackPanel.setBorder(BorderFactory.createTitledBorder("Playback"));
		playbackPanel.add(musicPlayer.createPlayer(), BorderLayout.NORTH);
		playbackPanel.add(clock.getComponent(), BorderLayout.CENTER);

		editButton = new JButton("Edit chord");
		editButton.setEnabled(false);
		editButton.addActionListener(event -> this.actions.editChord());
		JPanel songPanel = new JPanel(new BorderLayout());
		songPanel.setBorder(BorderFactory.createTitledBorder("Song information"));
		songPanel.add(fileInfo.get(), BorderLayout.CENTER);

		JPanel lowerPanel = new JPanel(new BorderLayout(8, 8));
		lowerPanel.add(playbackPanel, BorderLayout.CENTER);
		lowerPanel.add(editButton, BorderLayout.NORTH);
		lowerPanel.add(songPanel, BorderLayout.SOUTH);

		content.add(header, BorderLayout.NORTH);
		content.add(songView, BorderLayout.CENTER);
		content.add(lowerPanel, BorderLayout.SOUTH);
		frame.add(content, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
	}

	public void show() {
		frame.setVisible(true);
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setSongInfo(Song song) {
		fileInfo.set(song);
		String title = song.getName();
		songTitle.setText(title == null || title.isBlank() ? song.getFileName() : title);
	}

	public void setSongLoaded(boolean loaded) {
		viewMenu.setEnabled(loaded);
		saveMenuItem.setVisible(loaded);
		editButton.setEnabled(loaded);
		songViewLayout.show(songView, loaded ? "song" : "empty");
	}

	private JPanel createEmptyState() {
		JPanel emptyState = new JPanel();
		emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));
		emptyState.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new java.awt.Color(218, 224, 232)),
				BorderFactory.createEmptyBorder(32, 24, 32, 24)));

		JLabel heading = new JLabel("Start with a MIDI file", SwingConstants.CENTER);
		heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 24f));
		JLabel description = new JLabel(
				"Open a song to explore its chords and play along.",
				SwingConstants.CENTER);
		description.setForeground(new java.awt.Color(90, 99, 112));
		JButton openButton = new JButton("Choose MIDI file...");
		openButton.addActionListener(event -> chooseMidiFile(false));

		heading.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		description.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		openButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
		emptyState.add(Box.createVerticalGlue());
		emptyState.add(heading);
		emptyState.add(Box.createVerticalStrut(10));
		emptyState.add(description);
		emptyState.add(Box.createVerticalStrut(20));
		emptyState.add(openButton);
		emptyState.add(Box.createVerticalGlue());
		return emptyState;
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem importMenuItem = new JMenuItem("Import");
		saveMenuItem = new JMenuItem("Save");
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		viewMenu = new JMenu("View");
		JMenuItem showChordsItem = new JMenuItem("Show Unique Chords");

		importMenuItem.addActionListener(event -> chooseMidiFile(false));
		saveMenuItem.addActionListener(event -> chooseMidiFile(true));
		exitMenuItem.addActionListener(event ->
				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
		showChordsItem.addActionListener(event -> actions.showUniqueChords());

		saveMenuItem.setVisible(false);
		viewMenu.setEnabled(false);
		viewMenu.add(showChordsItem);
		fileMenu.add(importMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		return menuBar;
	}

	private void chooseMidiFile(boolean save) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("MIDI files (*.mid, *.midi)", "mid", "midi"));
		int result = save ? chooser.showSaveDialog(frame) : chooser.showOpenDialog(frame);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File selectedFile = chooser.getSelectedFile();
		if (save) {
			selectedFile = ensureMidiExtension(selectedFile);
			actions.saveMidi(selectedFile);
		} else {
			actions.importMidi(selectedFile);
		}
	}

	private File ensureMidiExtension(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		if (name.endsWith(".mid") || name.endsWith(".midi")) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + ".mid");
	}
}
