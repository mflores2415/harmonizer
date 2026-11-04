package harmonizer.ui;

import harmonizer.model.Song;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public final class FileInfoDisplay {
	private final GridPane view = new GridPane();
	private final Label fileName = new Label("-");
	private final Label songName = new Label("-");
	private final Label timeSignature = new Label("-");
	private final Label key = new Label("-");
	private final Label tempo = new Label("-");

	public FileInfoDisplay() {
		view.setHgap(16);
		view.setVgap(8);
		view.setPadding(new Insets(12));
		addRow(0, "File", fileName);
		addRow(1, "Song", songName);
		addRow(2, "Time signature", timeSignature);
		addRow(3, "Key", key);
		addRow(4, "Tempo", tempo);
	}

	private void addRow(int row, String title, Label value) {
		Label heading = new Label(title);
		heading.getStyleClass().add("info-heading");
		view.add(heading, 0, row);
		view.add(value, 1, row);
		GridPane.setHalignment(value, HPos.RIGHT);
	}

	public Node getView() {
		return view;
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
}
