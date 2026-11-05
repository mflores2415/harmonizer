package harmonizer.ui;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.Song;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** The main window. It shows state and forwards user requests to {@link Actions}. */
public final class HarmonizerWindow {
	/** What the user can ask for from the main window. */
	public interface Actions {
		void openSong(File file);
		void saveSong(File file);
		void showUniqueChords();
		void correctChordName();
		void transpose(int semitones);
		void changeSpeed(double speed);
		void playPause();
		void rewind();
		void restart();
		void changeAnalysisMode(ChordAnalysisMode mode);
		void changeInstrumentView(boolean enabled);
	}


	private final Stage stage;
	private final Actions actions;
	private final FileInfoDisplay fileInfo = new FileInfoDisplay();
	private final NoteRollDisplay noteRoll = new NoteRollDisplay();
	private final TransposeControl transposeControl;
	private final SpeedControl speedControl;
	private final StackPane songContent = new StackPane();
	private final VBox emptyState;
	private final VBox loadedState;
	private final MenuItem saveMenuItem = new MenuItem("Save");
	private final MenuItem uniqueChordsMenuItem = new MenuItem("Unique chords in this song…");
	private final MenuItem correctChordMenuItem = new MenuItem("Correct chord name…");
	private final RadioMenuItem detailedAnalysisItem = new RadioMenuItem("Detailed (show chord qualities)");
	private final RadioMenuItem steadyAnalysisItem = new RadioMenuItem("Steady (hold quality while root stays)");
	private final RadioMenuItem chordViewItem = new RadioMenuItem("Chords");
	private final RadioMenuItem instrumentViewItem = new RadioMenuItem("Instruments");
	private final Button playPauseButton = new Button("Play");
	private final Button rewindButton = new Button("⏪ 10s");
	private final Button restartButton = new Button("Restart");
	private final Label songTitle = new Label("MIDI chord explorer");

	public HarmonizerWindow(Stage stage, Actions actions, ChordDisplay chordDisplay, Clock clock) {
		this.stage = stage;
		this.actions = actions;
		transposeControl = new TransposeControl(actions::transpose);
		speedControl = new SpeedControl(stage, actions::changeSpeed);
		stage.setTitle("Harmonizer");
		stage.setMinWidth(720);
		stage.setMinHeight(560);

		emptyState = createEmptyState();
		loadedState = createLoadedState(chordDisplay);
		songContent.getChildren().add(emptyState);

		BorderPane root = new BorderPane();
		root.getStyleClass().add("app-root");
		root.setTop(createHeader());
		// The song content scrolls when the window is small so the player bar below stays visible.
		ScrollPane contentScroll = new ScrollPane(songContent);
		contentScroll.setFitToWidth(true);
		contentScroll.setFitToHeight(true);
		contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		contentScroll.getStyleClass().add("content-scroll");
		contentScroll.setMinHeight(0);
		root.setCenter(contentScroll);
		root.setBottom(createPlayerPanel(clock));

		Scene scene = new Scene(root, 920, 680);
		var stylesheet = HarmonizerWindow.class.getResource("/styles/harmonizer.css");
		if (stylesheet != null) {
			scene.getStylesheets().add(stylesheet.toExternalForm());
		}
		stage.setScene(scene);
		setSongControlsEnabled(false, false);
	}

	public void show() {
		stage.show();
	}

	public Stage getStage() {
		return stage;
	}

	/** Shows a loaded (or reloaded) song: its details, chords, instrument parts and transposition. */
	public void showSong(Song song) {
		fileInfo.show(song);
		String title = song.getName();
		songTitle.setText(title == null || title.isBlank() ? song.getFileName() : title);
		songContent.getChildren().setAll(loadedState);
		noteRoll.setParts(song.getInstrumentParts(), song.getBarGrid(), song.getSequence().getTickLength());
		transposeControl.show(song.getTranspose(), true);
		setSongControlsEnabled(true, song.hasChords());
	}

	private void setSongControlsEnabled(boolean loaded, boolean hasChords) {
		saveMenuItem.setDisable(!loaded);
		uniqueChordsMenuItem.setDisable(!loaded);
		correctChordMenuItem.setDisable(!loaded || !hasChords);
		playPauseButton.setDisable(!loaded);
		rewindButton.setDisable(!loaded);
		restartButton.setDisable(!loaded);
		speedControl.getView().setDisable(!loaded);
	}

	public void showPlaybackTick(long tick, boolean playing) {
		noteRoll.updateTick(tick, playing);
	}

	public void setInstrumentView(boolean enabled) {
		chordViewItem.setSelected(!enabled);
		instrumentViewItem.setSelected(enabled);
		noteRoll.getView().setVisible(enabled);
		noteRoll.getView().setManaged(enabled);
	}

	/**
	 * @param speed     1.0 is the song's own tempo
	 * @param songTempo the song's tempo in BPM
	 */
	public void showSpeed(double speed, int songTempo) {
		speedControl.show(speed, songTempo, true);
	}

	public void setAnalysisMode(ChordAnalysisMode mode) {
		detailedAnalysisItem.setSelected(mode == ChordAnalysisMode.DETAILED);
		steadyAnalysisItem.setSelected(mode == ChordAnalysisMode.STEADY);
	}

	public Optional<String> askChordName(String currentName, boolean flat, int bar) {
		return ChordNameDialog.ask(stage, currentName, flat, bar);
	}

	/** Asks before a transpose throws away the user's chord name corrections. */
	public boolean confirmTransposeDiscardsCorrections(int corrections) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.initOwner(stage);
		confirm.setTitle("Transpose song");
		confirm.setHeaderText("Transposing will undo your chord name corrections");
		confirm.setContentText("You corrected " + corrections + (corrections == 1 ? " chord name" : " chord names")
				+ ". After transposing, chords are detected again in the new key.");
		return confirm.showAndWait().filter(ButtonType.OK::equals).isPresent();
	}

	public void showError(Exception exception) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.initOwner(stage);
		alert.setTitle("MIDI error");
		alert.setHeaderText("Unable to process MIDI file");
		alert.setContentText(exception.toString());
		alert.showAndWait();
	}

	private VBox createHeader() {
		MenuItem openMenuItem = new MenuItem("Open MIDI/Karaoke...");
		openMenuItem.setOnAction(event -> chooseFileToOpen());
		saveMenuItem.setOnAction(event -> chooseFileToSave());
		Menu fileMenu = new Menu("File");
		fileMenu.getItems().addAll(openMenuItem, saveMenuItem);

		ToggleGroup displayModes = new ToggleGroup();
		chordViewItem.setToggleGroup(displayModes);
		instrumentViewItem.setToggleGroup(displayModes);
		chordViewItem.setSelected(true);
		chordViewItem.setOnAction(event -> actions.changeInstrumentView(false));
		instrumentViewItem.setOnAction(event -> actions.changeInstrumentView(true));
		Menu viewMenu = new Menu("View");
		viewMenu.getItems().addAll(chordViewItem, instrumentViewItem);

		Menu songMenu = new Menu("Song");
		songMenu.getItems().addAll(transposeControl.getMenuItems());

		uniqueChordsMenuItem.setOnAction(event -> actions.showUniqueChords());
		correctChordMenuItem.setOnAction(event -> actions.correctChordName());
		ToggleGroup analysisModes = new ToggleGroup();
		detailedAnalysisItem.setToggleGroup(analysisModes);
		steadyAnalysisItem.setToggleGroup(analysisModes);
		detailedAnalysisItem.setSelected(true);
		detailedAnalysisItem.setOnAction(event -> actions.changeAnalysisMode(ChordAnalysisMode.DETAILED));
		steadyAnalysisItem.setOnAction(event -> actions.changeAnalysisMode(ChordAnalysisMode.STEADY));
		Menu analysisMenu = new Menu("Chord detection");
		analysisMenu.getItems().addAll(detailedAnalysisItem, steadyAnalysisItem);
		Menu chordsMenu = new Menu("Chords");
		chordsMenu.getItems().addAll(uniqueChordsMenuItem, correctChordMenuItem, new SeparatorMenuItem(), analysisMenu);

		MenuBar menuBar = new MenuBar(fileMenu, viewMenu, songMenu, chordsMenu);
		Label appName = new Label("Harmonizer");
		appName.getStyleClass().add("app-title");
		songTitle.getStyleClass().add("song-title");
		Button openButton = new Button("Open MIDI/Karaoke");
		openButton.getStyleClass().add("primary-button");
		openButton.setOnAction(event -> chooseFileToOpen());

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox titleRow = new HBox(18, appName, spacer, songTitle, openButton);
		titleRow.setAlignment(Pos.CENTER_LEFT);
		titleRow.setPadding(new Insets(14, 24, 14, 24));
		return new VBox(menuBar, titleRow, new Separator());
	}

	private VBox createEmptyState() {
		Label heading = new Label("Start with a MIDI or karaoke file");
		heading.getStyleClass().add("empty-heading");
		Label description = new Label("Open a song to explore its chords and play along.");
		description.getStyleClass().add("subtle-text");
		Button openButton = new Button("Choose MIDI or karaoke file");
		openButton.getStyleClass().add("primary-button");
		openButton.setOnAction(event -> chooseFileToOpen());

		VBox card = new VBox(16, heading, description, openButton);
		card.setAlignment(Pos.CENTER);
		card.getStyleClass().add("empty-card");
		StackPane.setMargin(card, new Insets(28));
		VBox.setVgrow(card, Priority.ALWAYS);
		return card;
	}

	private VBox createLoadedState(ChordDisplay chordDisplay) {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox.setHgrow(fileInfo.getView(), Priority.SOMETIMES);
		HBox detailsRow = new HBox(10, fileInfo.getView(), spacer, transposeControl.getView());
		detailsRow.setAlignment(Pos.CENTER_LEFT);
		detailsRow.getStyleClass().add("details-row");

		StackPane analysisView = new StackPane(chordDisplay.getView(), noteRoll.getView());
		noteRoll.getView().setVisible(false);
		noteRoll.getView().setManaged(false);
		analysisView.setMinHeight(260);
		VBox loaded = new VBox(14, analysisView, detailsRow);
		loaded.setPadding(new Insets(18, 24, 18, 24));
		VBox.setVgrow(analysisView, Priority.ALWAYS);
		return loaded;
	}

	private HBox createPlayerPanel(Clock clock) {
		playPauseButton.getStyleClass().add("primary-button");
		playPauseButton.setMinWidth(86);
		playPauseButton.setOnAction(event -> actions.playPause());
		rewindButton.setOnAction(event -> actions.rewind());
		restartButton.setOnAction(event -> actions.restart());
		clock.setPlayingListener(playing -> playPauseButton.setText(playing ? "Pause" : "Play"));

		Node clockView = clock.getView();
		HBox.setHgrow(clockView, Priority.ALWAYS);
		HBox panel = new HBox(10, rewindButton, playPauseButton, restartButton, speedControl.getView(), clockView);
		panel.setAlignment(Pos.CENTER_LEFT);
		panel.getStyleClass().add("player-panel");
		panel.setPadding(new Insets(10, 24, 10, 24));
		return panel;
	}

	private void chooseFileToOpen() {
		File file = createFileChooser("Open MIDI or karaoke file").showOpenDialog(stage);
		if (file != null) {
			actions.openSong(file);
		}
	}

	private void chooseFileToSave() {
		File file = createFileChooser("Save MIDI file").showSaveDialog(stage);
		if (file != null) {
			actions.saveSong(withMidiExtension(file));
		}
	}

	private static FileChooser createFileChooser(String title) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
				"MIDI/Karaoke files (*.mid, *.midi, *.kar)", "*.mid", "*.midi", "*.kar"));
		samplesFolder().ifPresent(folder -> chooser.setInitialDirectory(folder.toFile()));
		return chooser;
	}

	/**
	 * The sample songs: {@code samples/} in the working directory when run from the project,
	 * or next to the application jar in the packaged app.
	 */
	private static Optional<Path> samplesFolder() {
		List<Path> candidates = new ArrayList<>();
		candidates.add(Path.of("samples"));
		try {
			Path codeSource = Path.of(HarmonizerWindow.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			candidates.add(codeSource.resolveSibling("samples"));
		} catch (URISyntaxException | SecurityException | NullPointerException exception) {
			// No code location available; the working directory is the only candidate.
		}
		return candidates.stream().filter(Files::isDirectory).map(Path::toAbsolutePath).findFirst();
	}

	private static File withMidiExtension(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		if (name.endsWith(".mid") || name.endsWith(".midi") || name.endsWith(".kar")) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + ".mid");
	}
}
