package harmonizer.ui;

import java.util.List;
import java.util.function.IntConsumer;

import harmonizer.model.Song;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/** The "Transpose − 0 +" stepper and its Song-menu items. Reports the requested semitones. */
final class TransposeControl {
	static final PseudoClass SHIFTED = PseudoClass.getPseudoClass("shifted");

	private final HBox view;
	private final Button downButton = new Button("−");
	private final Button upButton = new Button("+");
	private final Label value = new Label("0");
	private final MenuItem upItem = new MenuItem("Transpose up a semitone");
	private final MenuItem downItem = new MenuItem("Transpose down a semitone");
	private final MenuItem resetItem = new MenuItem("Back to original key");
	private int semitones;
	private boolean enabled;

	TransposeControl(IntConsumer onTranspose) {
		Label heading = new Label("TRANSPOSE");
		heading.getStyleClass().add("info-heading");
		downButton.getStyleClass().add("transpose-button");
		upButton.getStyleClass().add("transpose-button");
		downButton.setAccessibleText("Transpose down a semitone");
		upButton.setAccessibleText("Transpose up a semitone");
		value.getStyleClass().add("transpose-value");
		value.setMinWidth(30);
		value.setAlignment(Pos.CENTER);

		downButton.setOnAction(event -> onTranspose.accept(semitones - 1));
		upButton.setOnAction(event -> onTranspose.accept(semitones + 1));
		upItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Up"));
		upItem.setOnAction(event -> onTranspose.accept(semitones + 1));
		downItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Down"));
		downItem.setOnAction(event -> onTranspose.accept(semitones - 1));
		resetItem.setAccelerator(KeyCombination.keyCombination("Shortcut+0"));
		resetItem.setOnAction(event -> onTranspose.accept(0));

		view = new HBox(6, heading, downButton, value, upButton);
		view.setAlignment(Pos.CENTER);
		view.setMinWidth(Region.USE_PREF_SIZE);
		view.getStyleClass().add("transpose-control");
		Tooltip help = new Tooltip("Shift the whole song up (+) or down (−) in semitones.\n"
				+ "This changes what you hear, the detected chords and key,\n"
				+ "and what gets saved. Drums are not affected.");
		help.setShowDelay(Duration.millis(300));
		Tooltip.install(view, help);
		refresh();
	}

	Node getView() {
		return view;
	}

	List<MenuItem> getMenuItems() {
		return List.of(upItem, downItem, resetItem);
	}

	void show(int semitones, boolean enabled) {
		this.semitones = semitones;
		this.enabled = enabled;
		refresh();
	}

	private void refresh() {
		value.setText(semitones > 0 ? "+" + semitones : semitones < 0 ? "−" + -semitones : "0");
		value.pseudoClassStateChanged(SHIFTED, semitones != 0);
		downButton.setDisable(!enabled || semitones <= -Song.MAX_TRANSPOSE);
		upButton.setDisable(!enabled || semitones >= Song.MAX_TRANSPOSE);
		downItem.setDisable(downButton.isDisable());
		upItem.setDisable(upButton.isDisable());
		resetItem.setDisable(!enabled || semitones == 0);
	}
}
