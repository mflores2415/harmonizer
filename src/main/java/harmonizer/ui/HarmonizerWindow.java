package harmonizer.ui;

import java.io.File;
import java.util.List;
import java.util.Locale;

import harmonizer.model.BarGrid;
import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.InstrumentPart;
import harmonizer.model.Song;
import harmonizer.player.MusicPlayer;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class HarmonizerWindow {
	public interface Actions {
		void importMidi(File file);
		void saveMidi(File file);
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

	private static final PseudoClass SHIFTED = PseudoClass.getPseudoClass("shifted");
	private static final double[] SPEED_PRESETS = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5};
	// MIDI files without a tempo event play at 120 BPM.
	private static final int DEFAULT_TEMPO = 120;

	private final Stage stage;
	private final Actions actions;
	private final FileInfoDisplay fileInfo = new FileInfoDisplay();
	private final StackPane songContent = new StackPane();
	private final VBox emptyState;
	private final VBox loadedState;
	private final NoteRollDisplay instrumentDisplay = new NoteRollDisplay();
	private final MenuItem saveMenuItem = new MenuItem("Save");
	private final MenuItem uniqueChordsMenuItem = new MenuItem("Unique chords in this song…");
	private final MenuItem correctChordMenuItem = new MenuItem("Correct chord name…");
	private final MenuItem transposeUpMenuItem = new MenuItem("Transpose up a semitone");
	private final MenuItem transposeDownMenuItem = new MenuItem("Transpose down a semitone");
	private final MenuItem transposeResetMenuItem = new MenuItem("Back to original key");
	private final RadioMenuItem detailedAnalysisItem = new RadioMenuItem("Detailed (show chord qualities)");
	private final RadioMenuItem steadyAnalysisItem = new RadioMenuItem("Steady (hold quality while root stays)");
	private final RadioMenuItem chordDisplayItem = new RadioMenuItem("Chords");
	private final RadioMenuItem instrumentDisplayItem = new RadioMenuItem("Instruments");
	private final Button transposeDownButton = new Button("−");
	private final Button transposeUpButton = new Button("+");
	private final Label transposeValue = new Label("0");
	private final MenuButton speedButton = new MenuButton("Speed 100%");
	private int transpose;
	private double speed = 1.0;
	private int songTempo = DEFAULT_TEMPO;
	private final Button playPauseButton = new Button("Play");
	private final Button rewindButton = new Button("⏪ 10s");
	private final Button restartButton = new Button("Restart");
	private final Label songTitle = new Label("MIDI chord explorer");
	private boolean instrumentView;
	private boolean songLoaded;
	private boolean hasChords;

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
		songLoaded = loaded;
		this.hasChords = hasChords;
		songContent.getChildren().setAll(loaded ? loadedState : emptyState);
		saveMenuItem.setDisable(!loaded);
		uniqueChordsMenuItem.setDisable(!loaded);
		correctChordMenuItem.setDisable(!loaded || !hasChords);
		updateTransposeControls();
		playPauseButton.setDisable(!loaded);
		rewindButton.setDisable(!loaded);
		restartButton.setDisable(!loaded);
		speedButton.setDisable(!loaded);
	}

	public void setInstrumentParts(List<InstrumentPart> parts, BarGrid barGrid, long songEndTick) {
		instrumentDisplay.setParts(parts, barGrid, songEndTick);
	}

	public void setInstrumentTick(long tick, boolean playing) {
		instrumentDisplay.updateTick(tick, playing);
	}

	public void setInstrumentView(boolean enabled) {
		instrumentView = enabled;
		chordDisplayItem.setSelected(!enabled);
		instrumentDisplayItem.setSelected(enabled);
		instrumentDisplay.getView().setVisible(enabled);
		instrumentDisplay.getView().setManaged(enabled);
	}

	/**
	 * Shows the playback speed (1.0 = the song's own tempo) and the song's tempo in BPM,
	 * or 0 if the file doesn't say.
	 */
	public void setSpeed(double speed, int songTempo) {
		this.speed = speed;
		this.songTempo = songTempo > 0 ? songTempo : DEFAULT_TEMPO;
		String percent = Math.round(speed * 100) + "%";
		speedButton.setText(Math.abs(speed - 1.0) < 0.001
				? "Speed 100%"
				: "Speed " + percent + " · " + Math.round(this.songTempo * speed) + " BPM");
		speedButton.pseudoClassStateChanged(SHIFTED, Math.abs(speed - 1.0) >= 0.001);
	}

	/** Shows the current transposition, in semitones relative to the original file. */
	public void setTranspose(int semitones) {
		transpose = semitones;
		updateTransposeControls();
	}

	private void updateTransposeControls() {
		transposeValue.setText(transpose > 0 ? "+" + transpose : transpose < 0 ? "−" + -transpose : "0");
		transposeValue.pseudoClassStateChanged(SHIFTED, transpose != 0);
		transposeDownButton.setDisable(!songLoaded || transpose <= -Song.MAX_TRANSPOSE);
		transposeUpButton.setDisable(!songLoaded || transpose >= Song.MAX_TRANSPOSE);
		transposeDownMenuItem.setDisable(transposeDownButton.isDisable());
		transposeUpMenuItem.setDisable(transposeUpButton.isDisable());
		transposeResetMenuItem.setDisable(!songLoaded || transpose == 0);
	}

	public void setAnalysisMode(ChordAnalysisMode mode) {
		detailedAnalysisItem.setSelected(mode == ChordAnalysisMode.DETAILED);
		steadyAnalysisItem.setSelected(mode == ChordAnalysisMode.STEADY);
	}

	private VBox createHeader() {
		MenuItem openMenuItem = new MenuItem("Open MIDI/Karaoke...");
		openMenuItem.setOnAction(event -> chooseMidiFile(false));
		saveMenuItem.setDisable(true);
		saveMenuItem.setOnAction(event -> chooseMidiFile(true));
		Menu fileMenu = new Menu("File");
		fileMenu.getItems().addAll(openMenuItem, saveMenuItem);

		uniqueChordsMenuItem.setDisable(true);
		uniqueChordsMenuItem.setOnAction(event -> actions.showUniqueChords());
		correctChordMenuItem.setDisable(true);
		correctChordMenuItem.setOnAction(event -> actions.correctChordName());

		transposeUpMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Up"));
		transposeUpMenuItem.setOnAction(event -> actions.transpose(transpose + 1));
		transposeDownMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Down"));
		transposeDownMenuItem.setOnAction(event -> actions.transpose(transpose - 1));
		transposeResetMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+0"));
		transposeResetMenuItem.setOnAction(event -> actions.transpose(0));
		Menu songMenu = new Menu("Song");
		songMenu.getItems().addAll(transposeUpMenuItem, transposeDownMenuItem, transposeResetMenuItem);

		ToggleGroup analysisModes = new ToggleGroup();
		detailedAnalysisItem.setToggleGroup(analysisModes);
		steadyAnalysisItem.setToggleGroup(analysisModes);
		detailedAnalysisItem.setSelected(true);
		detailedAnalysisItem.setOnAction(event -> actions.changeAnalysisMode(ChordAnalysisMode.DETAILED));
		steadyAnalysisItem.setOnAction(event -> actions.changeAnalysisMode(ChordAnalysisMode.STEADY));
		Menu analysisMenu = new Menu("Chord detection");
		analysisMenu.getItems().addAll(detailedAnalysisItem, steadyAnalysisItem);
		Menu chordsMenu = new Menu("Chords");
		chordsMenu.getItems().addAll(uniqueChordsMenuItem, correctChordMenuItem,
				new SeparatorMenuItem(), analysisMenu);

		ToggleGroup displayModes = new ToggleGroup();
		chordDisplayItem.setToggleGroup(displayModes);
		instrumentDisplayItem.setToggleGroup(displayModes);
		chordDisplayItem.setSelected(true);
		chordDisplayItem.setOnAction(event -> actions.changeInstrumentView(false));
		instrumentDisplayItem.setOnAction(event -> actions.changeInstrumentView(true));
		Menu viewMenu = new Menu("View");
		viewMenu.getItems().addAll(chordDisplayItem, instrumentDisplayItem);

		MenuBar menuBar = new MenuBar(fileMenu, viewMenu, songMenu, chordsMenu);
		Label appName = new Label("Harmonizer");
		appName.getStyleClass().add("app-title");
		songTitle.getStyleClass().add("song-title");
		Button openButton = new Button("Open MIDI/Karaoke");
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
		Label heading = new Label("Start with a MIDI or karaoke file");
		heading.getStyleClass().add("empty-heading");
		Label description = new Label("Open a song to explore its chords and play along.");
		description.getStyleClass().add("subtle-text");
		Button openButton = new Button("Choose MIDI or karaoke file");
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
		Region actionsSpacer = new Region();
		HBox.setHgrow(actionsSpacer, Priority.ALWAYS);
		HBox.setHgrow(fileInfo.getView(), Priority.SOMETIMES);
		HBox detailsRow = new HBox(10, fileInfo.getView(), actionsSpacer, createTransposeControl());
		detailsRow.setAlignment(Pos.CENTER_LEFT);
		detailsRow.getStyleClass().add("details-row");

		StackPane analysisView = new StackPane(chordDisplay.getView(), instrumentDisplay.getView());
		instrumentDisplay.getView().setVisible(false);
		instrumentDisplay.getView().setManaged(false);
		analysisView.setMinHeight(260);
		VBox loaded = new VBox(14, analysisView, detailsRow);
		loaded.setPadding(new Insets(18, 24, 18, 24));
		VBox.setVgrow(analysisView, Priority.ALWAYS);
		return loaded;
	}

	private MenuButton createSpeedButton() {
		speedButton.getStyleClass().add("speed-button");
		speedButton.setDisable(true);
		speedButton.setMinWidth(Region.USE_PREF_SIZE);
		for (double preset : SPEED_PRESETS) {
			String label = Math.round(preset * 100) + "%" + (preset == 1.0 ? "  (original)"
					: preset == 0.5 ? "  (half speed)" : preset == 0.25 ? "  (quarter speed)" : "");
			MenuItem item = new MenuItem(label);
			item.setOnAction(event -> actions.changeSpeed(preset));
			speedButton.getItems().add(item);
		}
		MenuItem custom = new MenuItem("Custom tempo…");
		custom.setOnAction(event -> chooseCustomTempo());
		speedButton.getItems().addAll(new SeparatorMenuItem(), custom);
		Tooltip help = new Tooltip("Play slower (or faster) to practice.\n"
				+ "The pitch stays the same, and the saved file is not changed.");
		help.setShowDelay(Duration.millis(300));
		speedButton.setTooltip(help);
		return speedButton;
	}

	private void chooseCustomTempo() {
		int minimum = (int) Math.ceil(songTempo * MusicPlayer.MIN_SPEED);
		int maximum = (int) Math.floor(songTempo * MusicPlayer.MAX_SPEED);
		Spinner<Integer> bpm = new Spinner<>(minimum, maximum, (int) Math.round(songTempo * speed), 1);
		bpm.setEditable(true);
		bpm.setPrefWidth(110);
		// Commit typed text even if the user presses OK without leaving the field.
		bpm.focusedProperty().addListener((observable, wasFocused, focused) -> {
			if (!focused) {
				bpm.increment(0);
			}
		});

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
		dialog.initOwner(stage);
		dialog.setTitle("Custom tempo");
		dialog.setHeaderText("Practice at a custom tempo");
		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, apply);
		dialog.getDialogPane().lookupButton(apply).addEventFilter(javafx.event.ActionEvent.ACTION,
				event -> commitSpinner(bpm));
		dialog.showAndWait().filter(apply::equals)
				.ifPresent(result -> actions.changeSpeed(bpm.getValue() / (double) songTempo));
	}

	private static void commitSpinner(Spinner<Integer> spinner) {
		String text = spinner.getEditor().getText().trim();
		try {
			int value = Integer.parseInt(text);
			var factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
			factory.setValue(Math.max(factory.getMin(), Math.min(factory.getMax(), value)));
		} catch (NumberFormatException exception) {
			spinner.getEditor().setText(Integer.toString(spinner.getValue()));
		}
	}

	private HBox createTransposeControl() {
		Label heading = new Label("TRANSPOSE");
		heading.getStyleClass().add("info-heading");
		transposeDownButton.getStyleClass().add("transpose-button");
		transposeUpButton.getStyleClass().add("transpose-button");
		transposeDownButton.setAccessibleText("Transpose down a semitone");
		transposeUpButton.setAccessibleText("Transpose up a semitone");
		transposeDownButton.setOnAction(event -> actions.transpose(transpose - 1));
		transposeUpButton.setOnAction(event -> actions.transpose(transpose + 1));
		transposeValue.getStyleClass().add("transpose-value");
		transposeValue.setMinWidth(30);
		transposeValue.setAlignment(Pos.CENTER);

		HBox control = new HBox(6, heading, transposeDownButton, transposeValue, transposeUpButton);
		control.setAlignment(Pos.CENTER);
		control.setMinWidth(Region.USE_PREF_SIZE);
		control.getStyleClass().add("transpose-control");
		Tooltip help = new Tooltip("Shift the whole song up (+) or down (−) in semitones.\n"
				+ "This changes what you hear, the detected chords and key,\n"
				+ "and what gets saved. Drums are not affected.");
		help.setShowDelay(Duration.millis(300));
		Tooltip.install(control, help);
		updateTransposeControls();
		return control;
	}

	private HBox createPlayerPanel(Clock clock) {
		playPauseButton.setDisable(true);
		playPauseButton.getStyleClass().add("primary-button");
		playPauseButton.setMinWidth(86);
		playPauseButton.setOnAction(event -> actions.playPause());
		rewindButton.setDisable(true);
		rewindButton.setOnAction(event -> actions.rewind());
		restartButton.setDisable(true);
		restartButton.setOnAction(event -> actions.restart());
		clock.setPlayingListener(playing -> playPauseButton.setText(playing ? "Pause" : "Play"));

		Node clockView = clock.getView();
		HBox.setHgrow(clockView, Priority.ALWAYS);
		HBox panel = new HBox(10, rewindButton, playPauseButton, restartButton, createSpeedButton(), clockView);
		panel.setAlignment(Pos.CENTER_LEFT);
		panel.getStyleClass().add("player-panel");
		panel.setPadding(new Insets(10, 24, 10, 24));
		return panel;
	}

	private void chooseMidiFile(boolean save) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(save ? "Save MIDI file" : "Open MIDI or karaoke file");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(
						"MIDI/Karaoke files (*.mid, *.midi, *.kar)", "*.mid", "*.midi", "*.kar"));
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
		if (name.endsWith(".mid") || name.endsWith(".midi") || name.endsWith(".kar")) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + ".mid");
	}
}
