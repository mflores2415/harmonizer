package harmonizer.ui;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import harmonizer.model.Song;

public final class FileInfoDisplay {
	private final JPanel component;
	private final JLabel fileName;
	private final JLabel songName;
	private final JLabel timeSignature;
	private final JLabel key;
	private final JLabel tempo;

	public FileInfoDisplay() {
		component = new JPanel(new GridLayout(0, 2, 12, 4));
		fileName = new JLabel("-");
		songName = new JLabel("-");
		timeSignature = new JLabel("-");
		key = new JLabel("-");
		tempo = new JLabel("-");

		addRow("File", fileName);
		addRow("Song", songName);
		addRow("Time signature", timeSignature);
		addRow("Key", key);
		addRow("Tempo", tempo);
	}

	private void addRow(String label, JLabel value) {
		component.add(new JLabel(label + ":"));
		component.add(value);
	}

	public void set(Song song) {
		fileName.setText(song.getFileName());
		songName.setText(valueOrDash(song.getName()));
		timeSignature.setText(valueOrDash(song.getTimeSignature()));
		key.setText(valueOrDash(song.getKey()));
		tempo.setText(song.getTempo() > 0 ? song.getTempo() + " BPM" : "-");
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	public JPanel get() {
		return component;
	}
}
