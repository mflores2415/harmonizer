package harmonizer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class ChordDisplay {
	private final HBox view = new HBox(18);
	private final Label currentChord = new Label("—");
	private final Label nextChord = new Label("—");
	private String current = "—";
	private String next = "—";
	private long tick;

	public ChordDisplay() {
		view.setAlignment(Pos.CENTER);
		view.setPadding(new Insets(30, 12, 30, 12));
		view.getStyleClass().add("chord-display");
		view.getChildren().addAll(createCard("CURRENT CHORD", currentChord, true),
				createCard("UP NEXT", nextChord, false));
	}

	private VBox createCard(String heading, Label chord, boolean primary) {
		Label title = new Label(heading);
		title.getStyleClass().add("chord-heading");
		chord.getStyleClass().add(primary ? "current-chord" : "next-chord");
		VBox card = new VBox(12, title, chord);
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(28));
		card.getStyleClass().add("chord-card");
		card.getStyleClass().add(primary ? "current-card" : "next-card");
		HBox.setHgrow(card, Priority.ALWAYS);
		return card;
	}

	public Node getView() {
		return view;
	}

	public String getChords() {
		return current + " " + next;
	}

	public long getTick() {
		return tick;
	}

	public void set(String chordText, long chordTick) {
		String[] pair = chordText.split(" ", 2);
		current = pair[0];
		next = pair.length > 1 ? pair[1] : "";
		tick = chordTick;
		currentChord.setText(current);
		nextChord.setText(next);
	}

	public void showEmptyState() {
		current = "—";
		next = "—";
		tick = 0;
		currentChord.setText(current);
		nextChord.setText(next);
	}

}
