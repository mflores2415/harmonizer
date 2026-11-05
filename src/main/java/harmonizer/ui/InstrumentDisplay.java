package harmonizer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import harmonizer.model.InstrumentPart;
import harmonizer.model.InstrumentPart.NoteSpan;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public final class InstrumentDisplay {
	private static final double PIXELS_PER_BEAT = 72;
	private static final double NOTE_ROW_HEIGHT = 18;
	private static final double NOTE_TOP_PADDING = 24;
	private static final double PLAYHEAD_MARGIN = 70;
	private static final PseudoClass SOUNDING = PseudoClass.getPseudoClass("sounding");

	private final VBox view = new VBox(10);
	private final FlowPane instrumentOptions = new FlowPane(8, 8);
	private final Label currentNotes = new Label("Select an instrument part.");
	private final VBox timelineRows = new VBox();
	private final ScrollPane horizontalScroll = new ScrollPane(timelineRows);
	private final List<Line> playheads = new ArrayList<>();
	private final List<RenderedNote> renderedNotes = new ArrayList<>();
	private List<InstrumentPart> parts = List.of();
	private ToggleGroup instrumentGroup = new ToggleGroup();
	private InstrumentPart selectedPart;
	private double contentWidth;
	private double viewportWidth;
	private long songEndTick;
	private long currentTick;
	private int currentResolution = 480;
	private Line rulerPlayhead;
	private Line lanePlayhead;

	public InstrumentDisplay() {
		Label heading = new Label("INSTRUMENT PARTS");
		heading.getStyleClass().add("section-title");
		instrumentOptions.getStyleClass().add("instrument-options");

		currentNotes.getStyleClass().add("current-notes");
		currentNotes.setWrapText(true);
		currentNotes.setMinHeight(54);

		horizontalScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		horizontalScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		horizontalScroll.setFitToHeight(false);
		horizontalScroll.setPannable(true);
		horizontalScroll.setMinWidth(0);
		horizontalScroll.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
			viewportWidth = newBounds.getWidth();
			scrollToTick(currentTick, currentResolution);
		});
		VBox.setVgrow(horizontalScroll, Priority.ALWAYS);

		view.getStyleClass().add("instrument-display");
		view.getChildren().addAll(heading, instrumentOptions, currentNotes, horizontalScroll);
		showEmptyTimeline();
	}

	public Node getView() {
		return view;
	}

	public void setParts(List<InstrumentPart> parts, int resolution, long songEndTick) {
		this.parts = List.copyOf(parts);
		this.currentResolution = Math.max(1, resolution);
		this.songEndTick = Math.max(0, songEndTick);
		currentTick = 0;
		instrumentOptions.getChildren().clear();
		instrumentGroup = new ToggleGroup();
		selectedPart = null;
		renderedNotes.clear();
		playheads.clear();

		if (parts.isEmpty()) {
			instrumentOptions.setVisible(false);
			instrumentOptions.setManaged(false);
			currentNotes.setText("No instrument note events found.");
			showEmptyTimeline();
			return;
		}

		instrumentOptions.setVisible(true);
		instrumentOptions.setManaged(true);
		for (InstrumentPart part : parts) {
			RadioButton option = new RadioButton(part.getName());
			option.setToggleGroup(instrumentGroup);
			option.getStyleClass().add("instrument-option");
			option.setOnAction(event -> selectPart(part));
			instrumentOptions.getChildren().add(option);
			if (selectedPart == null) {
				selectedPart = part;
				option.setSelected(true);
			}
		}
		renderTimeline();
		updateTick(0);
	}

	public void updateTick(long tick) {
		currentTick = Math.max(0, tick);
		double x = 20 + currentTick / (double) currentResolution * PIXELS_PER_BEAT;
		if (rulerPlayhead != null) {
			rulerPlayhead.setTranslateX(x - 1);
		}
		if (lanePlayhead != null) {
			lanePlayhead.setTranslateX(x - 1);
		}
		renderedNotes.forEach(note -> note.rectangle().pseudoClassStateChanged(
				SOUNDING, note.span().startTick() <= currentTick && currentTick < note.span().endTick()));
		updateCurrentNotes();
		scrollToTick(currentTick, currentResolution);
	}

	private void selectPart(InstrumentPart part) {
		selectedPart = part;
		renderedNotes.clear();
		playheads.clear();
		renderTimeline();
		updateTick(currentTick);
	}

	private void renderTimeline() {
		timelineRows.getChildren().clear();
		renderedNotes.clear();
		playheads.clear();
		if (selectedPart == null) {
			showEmptyTimeline();
			return;
		}

		contentWidth = Math.max(640,
				songEndTick / (double) currentResolution * PIXELS_PER_BEAT + 48);
		Pane ruler = createRuler();
		Pane lane = createLane(selectedPart);
		timelineRows.getChildren().addAll(ruler, lane);
		rulerPlayhead = addPlayhead(ruler, 40);
		lanePlayhead = addPlayhead(lane, lane.getPrefHeight());
		playheads.add(rulerPlayhead);
		playheads.add(lanePlayhead);
	}

	private void showEmptyTimeline() {
		contentWidth = 640;
		Pane emptyPane = new Pane();
		emptyPane.setPrefSize(contentWidth, 240);
		emptyPane.setMinSize(contentWidth, 240);
		Label emptyMessage = new Label("Choose an instrument to see its notes.");
		emptyMessage.getStyleClass().add("subtle-text");
		emptyMessage.setLayoutX(20);
		emptyMessage.setLayoutY(24);
		emptyPane.getChildren().add(emptyMessage);
		timelineRows.getChildren().setAll(emptyPane);
		rulerPlayhead = null;
		lanePlayhead = null;
	}

	private Pane createRuler() {
		Pane ruler = new Pane();
		ruler.setPrefSize(contentWidth, 40);
		ruler.setMinSize(contentWidth, 40);
		ruler.getStyleClass().add("instrument-ruler");
		int totalBeats = (int) Math.ceil(songEndTick / (double) currentResolution);
		for (int beat = 0; beat <= totalBeats; beat++) {
			double x = 20 + beat * PIXELS_PER_BEAT;
			Line line = new Line(x, 0, x, 40);
			line.getStyleClass().add(beat % 4 == 0 ? "measure-line" : "beat-line");
			ruler.getChildren().add(line);
			if (beat % 4 == 0) {
				Label label = new Label(Integer.toString(beat + 1));
				label.getStyleClass().add("beat-label");
				label.setLayoutX(x + 4);
				label.setLayoutY(4);
				ruler.getChildren().add(label);
			}
		}
		return ruler;
	}

	private Pane createLane(InstrumentPart part) {
		int lowestPitch = Math.max(0, part.getLowestPitch() - 2);
		int highestPitch = Math.min(127, part.getHighestPitch() + 2);
		double laneHeight = (highestPitch - lowestPitch + 1) * NOTE_ROW_HEIGHT
				+ NOTE_TOP_PADDING * 2;
		Pane lane = new Pane();
		lane.setPrefSize(contentWidth, laneHeight);
		lane.setMinSize(contentWidth, laneHeight);
		lane.getStyleClass().add("instrument-lane");

		int totalBeats = (int) Math.ceil(songEndTick / (double) currentResolution);
		for (int beat = 0; beat <= totalBeats; beat++) {
			double x = 20 + beat * PIXELS_PER_BEAT;
			Line line = new Line(x, 0, x, laneHeight);
			line.getStyleClass().add(beat % 4 == 0 ? "measure-line" : "beat-line");
			lane.getChildren().add(line);
		}

		for (int pitch = lowestPitch; pitch <= highestPitch; pitch++) {
			double y = NOTE_TOP_PADDING + (highestPitch - pitch) * NOTE_ROW_HEIGHT;
			Line line = new Line(20, y, contentWidth, y);
			line.getStyleClass().add(Math.floorMod(pitch, 12) == 0 ? "octave-line" : "pitch-line");
			lane.getChildren().add(line);
			Label pitchLabel = new Label(InstrumentPart.formatNote(pitch));
			pitchLabel.getStyleClass().add("pitch-axis-label");
			pitchLabel.setLayoutX(1);
			pitchLabel.setLayoutY(y - 8);
			lane.getChildren().add(pitchLabel);
		}

		for (NoteSpan note : part.getNotes()) {
			double noteX = 20 + note.startTick() / (double) currentResolution * PIXELS_PER_BEAT;
			double noteWidth = Math.max(4, (note.endTick() - note.startTick())
					/ (double) currentResolution * PIXELS_PER_BEAT);
			double noteY = NOTE_TOP_PADDING + (highestPitch - note.pitch()) * NOTE_ROW_HEIGHT;
			Rectangle noteBlock = new Rectangle(noteWidth, NOTE_ROW_HEIGHT - 2);
			noteBlock.setX(noteX);
			noteBlock.setY(noteY);
			noteBlock.getStyleClass().add("note-block");
			noteBlock.setAccessibleText(InstrumentPart.formatNote(note.pitch()));
			lane.getChildren().add(noteBlock);
			renderedNotes.add(new RenderedNote(noteBlock, note));
			if (noteWidth >= 42) {
				Label pitch = new Label(InstrumentPart.formatNote(note.pitch()));
				pitch.getStyleClass().add("note-label");
				pitch.setLayoutX(noteX + 4);
				pitch.setLayoutY(noteY - 1);
				pitch.setMouseTransparent(true);
				lane.getChildren().add(pitch);
			}
		}
		return lane;
	}

	private Line addPlayhead(Pane lane, double height) {
		Line line = new Line(0, 0, 0, height);
		line.getStyleClass().add("playhead-line");
		line.setMouseTransparent(true);
		lane.getChildren().add(line);
		return line;
	}

	private void updateCurrentNotes() {
		if (selectedPart == null) {
			if (!parts.isEmpty()) {
				currentNotes.setText("Select an instrument part.");
			}
			return;
		}
		String notes = selectedPart.getNotesAt(currentTick).stream()
				.map(InstrumentPart::formatNote)
				.collect(Collectors.joining(", "));
		currentNotes.setText(notes.isEmpty() ? "Now playing: —" : "Now playing: " + notes);
	}

	private void scrollToTick(long tick, int resolution) {
		if (viewportWidth <= 0 || contentWidth <= viewportWidth) {
			return;
		}
		double playheadX = 20 + Math.max(0, tick) / (double) Math.max(1, resolution) * PIXELS_PER_BEAT;
		double scrollableWidth = contentWidth - viewportWidth;
		double currentLeft = horizontalScroll.getHvalue() * scrollableWidth;
		double currentRight = currentLeft + viewportWidth;
		if (playheadX < currentLeft || playheadX > currentRight - PLAYHEAD_MARGIN) {
			double targetLeft = Math.max(0, playheadX - viewportWidth * 0.3);
			horizontalScroll.setHvalue(Math.min(1, targetLeft / scrollableWidth));
		}
	}

	private record RenderedNote(Rectangle rectangle, NoteSpan span) {
	}
}
