package harmonizer.ui;

import java.util.Optional;

import harmonizer.player.MusicPlayer;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Asks for a practice tempo in BPM and returns it as a speed factor. */
final class TempoDialog {
	private TempoDialog() {
	}

	/**
	 * @param songTempo    the song's own tempo in BPM
	 * @param currentSpeed the speed now, used for the starting value
	 * @return the chosen speed (1.0 = the song's tempo), or empty if cancelled
	 */
	static Optional<Double> ask(Window owner, int songTempo, double currentSpeed) {
		int minimum = (int) Math.ceil(songTempo * MusicPlayer.MIN_SPEED);
		int maximum = (int) Math.floor(songTempo * MusicPlayer.MAX_SPEED);
		Spinner<Integer> bpm = new Spinner<>(minimum, maximum, (int) Math.round(songTempo * currentSpeed), 1);
		bpm.setEditable(true);
		bpm.setPrefWidth(110);

		Label explanation = new Label("This song's tempo is " + songTempo + " BPM. Choose any tempo from "
				+ minimum + " to " + maximum + " BPM. If the song changes tempo, every part is "
				+ "slowed down or sped up by the same amount.");
		explanation.setWrapText(true);
		explanation.setMaxWidth(320);
		explanation.getStyleClass().add("subtle-text");

		HBox field = new HBox(8, new Label("Tempo"), bpm, new Label("BPM"));
		field.setAlignment(Pos.CENTER_LEFT);
		VBox content = new VBox(14, explanation, field);
		content.setPadding(new Insets(18));

		ButtonType apply = new ButtonType("Set tempo", ButtonBar.ButtonData.OK_DONE);
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(owner);
		dialog.setTitle("Custom tempo");
		dialog.setHeaderText("Practice at a custom tempo");
		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, apply);
		// Use typed text even if the user presses the button without leaving the field.
		dialog.getDialogPane().lookupButton(apply).addEventFilter(ActionEvent.ACTION, event -> commitTypedValue(bpm));
		return dialog.showAndWait()
				.filter(apply::equals)
				.map(result -> bpm.getValue() / (double) songTempo);
	}

	private static void commitTypedValue(Spinner<Integer> spinner) {
		var factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
		try {
			int typed = Integer.parseInt(spinner.getEditor().getText().trim());
			factory.setValue(Math.clamp(typed, factory.getMin(), factory.getMax()));
		} catch (NumberFormatException exception) {
			spinner.getEditor().setText(Integer.toString(spinner.getValue()));
		}
	}
}
