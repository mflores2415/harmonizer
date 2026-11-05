package harmonizer.ui;

import harmonizer.model.Song;
import harmonizer.model.SongKey;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public final class FileInfoDisplay {
	private final FlowPane view = new FlowPane(18, 6);
	private final Label fileName = new Label("-");
	private final Label songName = new Label("-");
	private final Label timeSignature = new Label("-");
	private final Label key = new Label("-");
	private final Label tempo = new Label("-");

	public FileInfoDisplay() {
		view.setAlignment(Pos.CENTER_LEFT);
		view.getStyleClass().add("song-details");
		view.setMinWidth(0);
		view.setPrefWrapLength(520);
		addItem("Key", key);
		addItem("Time", timeSignature);
		addItem("Tempo", tempo);
		addItem("Song", songName);
		addItem("File", fileName);
	}

	private void addItem(String title, Label value) {
		Label heading = new Label(title.toUpperCase());
		heading.getStyleClass().add("info-heading");
		value.getStyleClass().add("info-value");
		HBox item = new HBox(6, heading, value);
		item.setAlignment(Pos.BASELINE_LEFT);
		view.getChildren().add(item);
	}

	public Node getView() {
		return view;
	}

	public void show(Song song) {
		fileName.setText(song.getFileName());
		songName.setText(valueOrDash(song.getName()));
		timeSignature.setText(valueOrDash(song.getTimeSignature()));
		var grid = song.getBarGrid();
		timeSignature.setTooltip(grid.isAssumed()
				? new Tooltip("This file has no time signature, so 4/4 is assumed\n"
						+ "(the MIDI standard's default).")
				: grid.getSections().size() > 1 ? new Tooltip(grid.describeChanges()) : null);
		SongKey songKey = song.getKey();
		key.setText(valueOrDash(songKey.name()) + (songKey.estimated() ? " (estimated)" : ""));
		key.setTooltip(songKey.estimated()
				? new Tooltip("This file has no key, or its key doesn't match the notes,\n"
						+ "so the key was estimated from the notes that are played.")
				: null);
		tempo.setText(song.getTempo() > 0 ? song.getTempo() + " BPM" : "-");
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
