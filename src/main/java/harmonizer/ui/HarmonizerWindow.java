package harmonizer.ui;

import java.io.File;
import java.util.Locale;

import harmonizer.model.Song;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class HarmonizerWindow {
	public interface Actions {
		void importMidi(File file);
		void saveMidi(File file);
		void showUniqueChords();
		void editChord();
		void playPause();
		void rewind();
		void restart();
	}

	private final Stage stage;
	private final Actions actions;
	private final FileInfoDisplay fileInfo = new FileInfoDisplay();
	private final StackPane songContent = new StackPane();
	private final VBox emptyState;
	private final VBox loadedState;
	private final MenuItem saveMenuItem = new MenuItem("Save");
	private final MenuItem uniqueChordsMenuItem = new MenuItem("Show unique chords");
	private final Button showChordsButton = new Button("Unique chords");
	private final Button editChordButton = new Button("Edit chord");
	private final Button playPauseButton = new Button("Play");
	private final Button rewindButton = new Button("⏪ 10s");
	private final Button restartButton = new Button("Restart");
	private final Label songTitle = new Label("MIDI chord explorer");

	public HarmonizerWindow(Stage stage, Actions actions, ChordDisplay chordDisplay, Clock clock) {
		this.stage = stage;
		this.actions = actions;
		stage.setTitle("Harmonizer");
		stage.setMinWidth(720);
		stage.setMinHeight(560);

		emptyState = createEmptyState();
		loadedState = createLoadedState(chordDisplay);
		songContent.getChildren().add(emptyState);

		BorderPane root = new BorderPane();
		root.getStyleClass().add("app-root");
		root.setTop(createHeader());
		root.setCenter(songContent);
		root.setBottom(createPlayerPanel(clock));

		Scene scene = new Scene(root, 920, 680);
		var stylesheet = HarmonizerWindow.class.getResource("/styles/harmonizer.css");
		if (stylesheet != null) {
			scene.getStylesheets().add(stylesheet.toExternalForm());
		}
		stage.setScene(scene);
	}

	public void show() {
		stage.show();
	}

	public Stage getStage() {
		return stage;
	}

	public void setSongInfo(Song song) {
		fileInfo.set(song);
		String title = song.getName();
		songTitle.setText(title == null || title.isBlank() ? song.getFileName() : title);
	}

	public void setSongLoaded(boolean loaded, boolean hasChords) {
		songContent.getChildren().setAll(loaded ? loadedState : emptyState);
		saveMenuItem.setDisable(!loaded);
		uniqueChordsMenuItem.setDisable(!loaded);
		showChordsButton.setDisable(!loaded);
		editChordButton.setDisable(!loaded || !hasChords);
		playPauseButton.setDisable(!loaded);
		rewindButton.setDisable(!loaded);
		restartButton.setDisable(!loaded);
	}

	private VBox createHeader() {
		MenuItem openMenuItem = new MenuItem("Open MIDI...");
		openMenuItem.setOnAction(event -> chooseMidiFile(false));
		saveMenuItem.setDisable(true);
		saveMenuItem.setOnAction(event -> chooseMidiFile(true));
		Menu fileMenu = new Menu("File");
		fileMenu.getItems().addAll(openMenuItem, saveMenuItem);

		showChordsButton.setDisable(true);
		showChordsButton.setOnAction(event -> actions.showUniqueChords());
		Menu viewMenu = new Menu("View");
		uniqueChordsMenuItem.setDisable(true);
		uniqueChordsMenuItem.setOnAction(event -> actions.showUniqueChords());
		viewMenu.getItems().add(uniqueChordsMenuItem);

		MenuBar menuBar = new MenuBar(fileMenu, viewMenu);
		Label appName = new Label("Harmonizer");
		appName.getStyleClass().add("app-title");
		songTitle.getStyleClass().add("song-title");
		Button openButton = new Button("Open MIDI");
		openButton.getStyleClass().add("primary-button");
		openButton.setOnAction(event -> chooseMidiFile(false));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox titleRow = new HBox(18, appName, spacer, songTitle, openButton);
		titleRow.setAlignment(Pos.CENTER_LEFT);
		titleRow.setPadding(new Insets(14, 24, 14, 24));

		VBox header = new VBox(menuBar, titleRow, new Separator());
		return header;
	}

	private VBox createEmptyState() {
		Label heading = new Label("Start with a MIDI file");
		heading.getStyleClass().add("empty-heading");
		Label description = new Label("Open a song to explore its chords and play along.");
		description.getStyleClass().add("subtle-text");
		Button openButton = new Button("Choose MIDI file");
		openButton.getStyleClass().add("primary-button");
		openButton.setOnAction(event -> chooseMidiFile(false));

		VBox card = new VBox(16, heading, description, openButton);
		card.setAlignment(Pos.CENTER);
		card.getStyleClass().add("empty-card");
		StackPane.setMargin(card, new Insets(28));
		VBox.setVgrow(card, Priority.ALWAYS);
		return card;
	}

	private VBox createLoadedState(ChordDisplay chordDisplay) {
		HBox actionsRow = new HBox(10, editChordButton, showChordsButton);
		actionsRow.setAlignment(Pos.CENTER_RIGHT);

		VBox infoCard = new VBox(8, new Label("SONG DETAILS"), fileInfo.getView());
		infoCard.getStyleClass().add("info-card");

		VBox loaded = new VBox(20, chordDisplay.getView(), actionsRow, infoCard);
		loaded.setPadding(new Insets(24));
		VBox.setVgrow(chordDisplay.getView(), Priority.ALWAYS);
		return loaded;
	}

	private VBox createPlayerPanel(Clock clock) {
		Label sectionTitle = new Label("PLAYER");
		sectionTitle.getStyleClass().add("section-title");
		playPauseButton.setDisable(true);
		playPauseButton.setOnAction(event -> actions.playPause());
		rewindButton.setDisable(true);
		rewindButton.setOnAction(event -> actions.rewind());
		restartButton.setDisable(true);
		restartButton.setOnAction(event -> actions.restart());
		clock.setPlayingListener(playing -> playPauseButton.setText(playing ? "Pause" : "Play"));

		HBox controls = new HBox(10, rewindButton, playPauseButton, restartButton);
		controls.setAlignment(Pos.CENTER);
		VBox panel = new VBox(8, sectionTitle, controls, clock.getView());
		panel.getStyleClass().add("player-panel");
		panel.setPadding(new Insets(14, 24, 18, 24));
		return panel;
	}

	private void chooseMidiFile(boolean save) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(save ? "Save MIDI file" : "Open MIDI file");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("MIDI files (*.mid, *.midi)", "*.mid", "*.midi"));
		File selectedFile = save ? chooser.showSaveDialog(stage) : chooser.showOpenDialog(stage);
		if (selectedFile == null) {
			return;
		}
		if (save) {
			actions.saveMidi(ensureMidiExtension(selectedFile));
		} else {
			actions.importMidi(selectedFile);
		}
	}

	private File ensureMidiExtension(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		if (name.endsWith(".mid") || name.endsWith(".midi")) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + ".mid");
	}
}
