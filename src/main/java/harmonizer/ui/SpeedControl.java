package harmonizer.ui;

import java.util.function.DoubleConsumer;

import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Duration;

/** The player's "Speed" menu: preset speeds plus a custom tempo. Reports the requested speed. */
final class SpeedControl {
	private static final double[] PRESETS = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5};

	private final MenuButton button = new MenuButton();
	private double speed = 1.0;
	private int songTempo;

	SpeedControl(Window owner, DoubleConsumer onSpeed) {
		button.getStyleClass().add("speed-button");
		button.setMinWidth(Region.USE_PREF_SIZE);
		for (double preset : PRESETS) {
			MenuItem item = new MenuItem(presetLabel(preset));
			item.setOnAction(event -> onSpeed.accept(preset));
			button.getItems().add(item);
		}
		MenuItem custom = new MenuItem("Custom tempo…");
		custom.setOnAction(event -> TempoDialog.ask(owner, songTempo, speed).ifPresent(onSpeed::accept));
		button.getItems().addAll(new SeparatorMenuItem(), custom);

		Tooltip help = new Tooltip("Play slower (or faster) to practice.\n"
				+ "The pitch stays the same, and the saved file is not changed.");
		help.setShowDelay(Duration.millis(300));
		button.setTooltip(help);
		show(1.0, 0, false);
	}

	private static String presetLabel(double preset) {
		String suffix = preset == 1.0 ? "  (original)"
				: preset == 0.5 ? "  (half speed)"
				: preset == 0.25 ? "  (quarter speed)"
				: "";
		return Math.round(preset * 100) + "%" + suffix;
	}

	Node getView() {
		return button;
	}

	/**
	 * @param speed     1.0 is the song's own tempo
	 * @param songTempo the song's tempo in BPM
	 */
	void show(double speed, int songTempo, boolean enabled) {
		this.speed = speed;
		this.songTempo = songTempo;
		boolean original = Math.abs(speed - 1.0) < 0.001;
		button.setText(original ? "Speed 100%"
				: "Speed " + Math.round(speed * 100) + "% · " + Math.round(songTempo * speed) + " BPM");
		button.pseudoClassStateChanged(TransposeControl.SHIFTED, !original);
		button.setDisable(!enabled);
	}
}
