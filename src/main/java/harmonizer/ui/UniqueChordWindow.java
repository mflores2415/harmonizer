package harmonizer.ui;

import java.util.Comparator;
import java.util.Set;

import harmonizer.model.Chord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class UniqueChordWindow {
	private final Stage stage = new Stage();

	public UniqueChordWindow(Window owner, Set<Chord> chords) {
		stage.initOwner(owner);
		stage.setTitle("Unique chords");

		Label title = new Label("Chords in this song");
		title.getStyleClass().add("dialog-title");
		Label count = new Label(chords.size() + " unique chords");
		count.getStyleClass().add("subtle-text");

		FlowPane chordList = new FlowPane(10, 10);
		chordList.setAlignment(Pos.CENTER_LEFT);
		chordList.getChildren().addAll(chords.stream()
				.sorted(Comparator.comparing(Chord::getName))
				.map(chord -> {
					Label item = new Label(chord.getName());
					item.getStyleClass().add("chord-chip");
					return item;
				})
				.toList());

		VBox content = new VBox(12, title, count, chordList);
		content.setPadding(new Insets(22));
		content.getStyleClass().add("dialog-root");
		stage.setScene(new Scene(content, 480, 360));
		stage.setMinWidth(360);
		stage.setMinHeight(240);
	}

	public void show() {
		stage.show();
	}
}
