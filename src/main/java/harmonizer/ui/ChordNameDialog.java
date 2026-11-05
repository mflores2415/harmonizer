package harmonizer.ui;

import java.util.Comparator;
import java.util.Optional;

import harmonizer.model.Chord;
import harmonizer.model.Pitch;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Asks for a corrected chord name. It only asks; applying the name is up to the caller. */
public final class ChordNameDialog {
	private ChordNameDialog() {
	}

	/**
	 * @param currentName the chord's name now, e.g. "Cmaj"
	 * @param flat        whether to offer roots spelled with flats
	 * @param bar         the bar the chord starts in, for the header
	 * @return the chosen name, or empty if cancelled
	 */
	public static Optional<String> ask(Window owner, String currentName, boolean flat, int bar) {
		String quality = Chord.QUALITY_NAMES.stream()
				.filter(currentName::endsWith)
				.max(Comparator.comparingInt(String::length))
				.orElse(Chord.QUALITY_NAMES.get(0));
		String root = currentName.endsWith(quality)
				? currentName.substring(0, currentName.length() - quality.length())
				: Pitch.name(0, flat);

		ComboBox<String> rootChoice = new ComboBox<>(FXCollections.observableArrayList(Pitch.names(flat)));
		rootChoice.setValue(Pitch.names(flat).contains(root) ? root : Pitch.name(0, flat));
		ComboBox<String> qualityChoice = new ComboBox<>(FXCollections.observableArrayList(Chord.QUALITY_NAMES));
		qualityChoice.setValue(quality);

		Label explanation = new Label("Use this when the detected chord name is wrong. Only the name "
				+ "shown (and saved with the file) changes; the music itself is not changed.\n\n"
				+ "To play the song in a different key, use Song › Transpose instead.");
		explanation.setWrapText(true);
		explanation.setMaxWidth(340);
		explanation.getStyleClass().add("subtle-text");

		GridPane form = new GridPane();
		form.setHgap(12);
		form.setVgap(12);
		form.setPadding(new Insets(18));
		form.add(explanation, 0, 0, 2, 1);
		form.add(new Separator(), 0, 1, 2, 1);
		form.add(new Label("Detected as"), 0, 2);
		form.add(new Label(currentName), 1, 2);
		form.add(new Label("Root"), 0, 3);
		form.add(rootChoice, 1, 3);
		form.add(new Label("Quality"), 0, 4);
		form.add(qualityChoice, 1, 4);

		ButtonType saveName = new ButtonType("Save name", ButtonBar.ButtonData.OK_DONE);
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(owner);
		dialog.setTitle("Correct chord name");
		dialog.setHeaderText("Correct the chord name at bar " + bar);
		dialog.getDialogPane().setContent(form);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveName);
		return dialog.showAndWait()
				.filter(saveName::equals)
				.map(result -> rootChoice.getValue() + qualityChoice.getValue());
	}
}
