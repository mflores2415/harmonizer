package harmonizer.ui;

import harmonizer.midi.ChordLyrics;
import harmonizer.midi.ChordPair;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The "Current chord" and "Up next" cards. */
public final class ChordDisplay {
	private static final String NONE = "—";

	private final HBox view = new HBox(18);
	private final Label currentChord = new Label(NONE);
	private final Label nextChord = new Label(NONE);

	public ChordDisplay() {
		view.setAlignment(Pos.CENTER);
		view.setPadding(new Insets(12));
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
		card.getStyleClass().addAll("chord-card", primary ? "current-card" : "next-card");
		HBox.setHgrow(card, Priority.ALWAYS);
		return card;
	}

	/** Adds a right-click "Correct chord name…" shortcut on the chord cards. */
	public void setOnCorrectChordName(Runnable action) {
		MenuItem correct = new MenuItem("Correct chord name…");
		correct.setOnAction(event -> action.run());
		ContextMenu menu = new ContextMenu(correct);
		view.setOnContextMenuRequested(event -> menu.show(view, event.getScreenX(), event.getScreenY()));
	}

	public Node getView() {
		return view;
	}

	public void show(ChordPair chords) {
		currentChord.setText(chords.current());
		nextChord.setText(ChordLyrics.END.equals(chords.next()) ? NONE : chords.next());
	}

	public void showEmptyState() {
		currentChord.setText(NONE);
		nextChord.setText(NONE);
	}
}
