package harmonizer.ui;

import java.util.function.BiConsumer;

import harmonizer.model.Song;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public final class EditChordWindow {
	private static final String[] SHARP_ROOTS = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};
	private static final String[] FLAT_ROOTS = {
			"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
	};
	private static final String[] TYPES = {
			"maj", "min", "aug", "dim", "maj(m7)", "maj(M7)", "min(m7)", "min(M7)"
	};

	private EditChordWindow() {
	}

	public static void show(Window owner, Song song, long tick, String chordText,
			BiConsumer<String, Long> chordEdited) {
		if (song.getMap().isEmpty()) {
			return;
		}
		long chordTick = song.getMap().containsKey(tick) ? tick : song.getFirstTick();
		if (chordTick < 0 || !song.getMap().containsKey(chordTick)) {
			return;
		}

		String currentChord = song.getMap().get(chordTick).getName();
		long previousTick = song.getPrevTick(chordTick);
		String previousChord = previousTick < 0 ? null : song.getMap().get(previousTick).getName();
		String[] roots = song.isFlatKey() ? FLAT_ROOTS : SHARP_ROOTS;
		String[] oldPair = chordText.split(" ", 2);
		String currentType = chordType(currentChord);
		if (currentType.isEmpty()) {
			return;
		}
		String currentRoot = currentChord.substring(0, currentChord.length() - currentType.length());

		ComboBox<String> rootChoice = new ComboBox<>(FXCollections.observableArrayList(roots));
		rootChoice.setValue(currentRoot);
		ComboBox<String> typeChoice = new ComboBox<>(FXCollections.observableArrayList(TYPES));
		typeChoice.setValue(currentType);

		GridPane form = new GridPane();
		form.setHgap(12);
		form.setVgap(12);
		form.setPadding(new Insets(18));
		form.add(new Label("Current chord"), 0, 0);
		form.add(new Label(currentChord), 1, 0);
		form.add(new Separator(), 0, 1, 2, 1);
		form.add(new Label("Root"), 0, 2);
		form.add(rootChoice, 1, 2);
		form.add(new Label("Quality"), 0, 3);
		form.add(typeChoice, 1, 3);

		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(owner);
		dialog.setTitle("Edit chord");
		dialog.setHeaderText("Change chord at tick " + chordTick);
		dialog.getDialogPane().setContent(form);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.APPLY);
		dialog.showAndWait().filter(ButtonType.APPLY::equals).ifPresent(result -> {
			String editedChord = rootChoice.getValue() + typeChoice.getValue();
			String nextChord = oldPair.length > 1 ? oldPair[1] : "";
			String newChordText = editedChord + " " + nextChord;
			String oldChordText = currentChord + " " + nextChord;
			String previousOldText = previousChord + " " + currentChord;
			String previousNewText = previousChord + " " + editedChord;
			song.editChord(chordTick, newChordText, oldChordText, previousTick,
					previousOldText, previousNewText);
			chordEdited.accept(newChordText, chordTick);
		});
	}

	private static String chordType(String chord) {
		for (String type : TYPES) {
			if (chord.endsWith(type)) {
				return type;
			}
		}
		return "";
	}
}
