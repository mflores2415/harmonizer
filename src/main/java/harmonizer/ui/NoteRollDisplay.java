package harmonizer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import harmonizer.model.BarGrid;
import harmonizer.model.InstrumentPart;
import harmonizer.model.InstrumentPart.NoteSpan;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

/**
 * Canvas-based piano roll with a fixed playhead. Notes scroll right-to-left past the playhead,
 * a keyboard on the left lights up sounding pitches, and the pitch range always fits the height.
 */
public final class NoteRollDisplay {
	private static final double KEYBOARD_WIDTH = 64;
	private static final double DRUM_LABEL_WIDTH = 132;
	private static final double HEADER_HEIGHT = 22;
	private static final double PLAYHEAD_FRACTION = 0.25;
	private static final double MAX_ROW_HEIGHT = 28;
	private static final int MIN_PITCH_SPAN = 13;
	private static final double MIN_BEATS_VISIBLE = 4;
	private static final double MAX_BEATS_VISIBLE = 64;
	private static final long MAX_EXTRAPOLATION_NANOS = 250_000_000L;

	private static final Color BACKGROUND = Color.web("#ffffff");
	private static final Color BLACK_KEY_ROW = Color.web("#f4f7fb");
	private static final Color HEADER_BACKGROUND = Color.web("#edf4fd");
	private static final Color GRID_BEAT = Color.web("#edf0f4");
	private static final Color GRID_BAR = Color.web("#cbd6e3");
	private static final Color GRID_OCTAVE = Color.web("#d7e0e9");
	private static final Color TEXT_MUTED = Color.web("#52647c");
	private static final Color TEXT_DARK = Color.web("#193958");
	private static final Color NOTE_FILL = Color.web("#83b4e8");
	private static final Color NOTE_STROKE = Color.web("#397bb9");
	private static final Color NOTE_PAST_FILL = Color.web("#d6e3f2");
	private static final Color NOTE_PAST_STROKE = Color.web("#b3c7de");
	private static final Color NOTE_SOUNDING_FILL = Color.web("#f2b84b");
	private static final Color NOTE_SOUNDING_STROKE = Color.web("#c17a15");
	private static final Color WHITE_KEY = Color.web("#ffffff");
	private static final Color BLACK_KEY = Color.web("#2b3440");
	private static final Color KEY_BORDER = Color.web("#c3ceda");
	private static final Color PLAYHEAD = Color.web("#d84a4a");

	private static final Font SMALL_FONT = Font.font("Segoe UI", 10);
	private static final Font LABEL_FONT = Font.font("Segoe UI", FontWeight.BOLD, 11);
	private static final Font EMPTY_FONT = Font.font("Segoe UI", 14);

	private static final String[] GM_DRUMS = {
			"Acoustic Bass Drum", "Bass Drum", "Side Stick", "Acoustic Snare", "Hand Clap",
			"Electric Snare", "Low Floor Tom", "Closed Hi-Hat", "High Floor Tom", "Pedal Hi-Hat",
			"Low Tom", "Open Hi-Hat", "Low-Mid Tom", "Hi-Mid Tom", "Crash Cymbal 1", "High Tom",
			"Ride Cymbal 1", "Chinese Cymbal", "Ride Bell", "Tambourine", "Splash Cymbal",
			"Cowbell", "Crash Cymbal 2", "Vibraslap", "Ride Cymbal 2", "Hi Bongo", "Low Bongo",
			"Mute Hi Conga", "Open Hi Conga", "Low Conga", "High Timbale", "Low Timbale",
			"High Agogo", "Low Agogo", "Cabasa", "Maracas", "Short Whistle", "Long Whistle",
			"Short Guiro", "Long Guiro", "Claves", "Hi Wood Block", "Low Wood Block",
			"Mute Cuica", "Open Cuica", "Mute Triangle", "Open Triangle"
	};
	private static final int FIRST_GM_DRUM = 35;

	private final VBox view = new VBox(10);
	private final ComboBox<InstrumentPart> partPicker = new ComboBox<>();
	private final FlowPane nowPlaying = new FlowPane(6, 6);
	private final Canvas canvas = new Canvas();
	private final AnimationTimer animation;

	private List<InstrumentPart> parts = List.of();
	private InstrumentPart selectedPart;
	private int resolution = 480;
	private BarGrid barGrid;
	private long songEndTick;
	private double beatsVisible = 12;

	// Pitch rows for the selected part: row 0 is drawn at the top.
	private int[] rowPitches = new int[0];
	private final Map<Integer, Integer> rowByPitch = new HashMap<>();

	// Playback smoothing: the player reports ticks ~10x per second, we interpolate between them.
	private long reportedTick;
	private long reportedAtNanos;
	private double ticksPerNano;
	private boolean playing;
	private boolean dirty = true;
	private List<Integer> shownPitches = List.of();

	public NoteRollDisplay() {
		Label heading = new Label("INSTRUMENT");
		heading.getStyleClass().add("section-title");
		heading.setMinWidth(Region.USE_PREF_SIZE);

		partPicker.getStyleClass().add("part-picker");
		partPicker.setMinWidth(140);
		partPicker.setPromptText("Choose an instrument part");
		partPicker.setMaxWidth(360);
		partPicker.setConverter(new StringConverter<>() {
			@Override
			public String toString(InstrumentPart part) {
				return part == null ? "" : part.getName();
			}

			@Override
			public InstrumentPart fromString(String text) {
				return null;
			}
		});
		partPicker.valueProperty().addListener((observable, previous, part) -> selectPart(part));

		Label nowPlayingHeading = new Label("NOW PLAYING");
		nowPlayingHeading.getStyleClass().add("section-title");
		nowPlayingHeading.setMinWidth(Region.USE_PREF_SIZE);
		nowPlaying.getStyleClass().add("now-playing");
		nowPlaying.setAlignment(Pos.CENTER_LEFT);
		nowPlaying.setPrefWrapLength(260);
		nowPlaying.setMinHeight(30);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox header = new HBox(10, heading, partPicker, spacer, nowPlayingHeading, nowPlaying);
		header.setAlignment(Pos.CENTER_LEFT);

		Pane canvasHolder = new Pane(canvas);
		canvasHolder.getStyleClass().add("note-roll");
		canvasHolder.setMinSize(0, 200);
		canvas.widthProperty().bind(canvasHolder.widthProperty());
		canvas.heightProperty().bind(canvasHolder.heightProperty());
		canvas.widthProperty().addListener(observable -> dirty = true);
		canvas.heightProperty().addListener(observable -> dirty = true);
		canvas.setOnScroll(event -> {
			if (event.getDeltaY() == 0) {
				return;
			}
			double factor = event.getDeltaY() > 0 ? 0.85 : 1 / 0.85;
			beatsVisible = clamp(beatsVisible * factor, MIN_BEATS_VISIBLE, MAX_BEATS_VISIBLE);
			dirty = true;
			event.consume();
		});
		VBox.setVgrow(canvasHolder, Priority.ALWAYS);

		Label hint = new Label("Scroll over the roll to zoom in or out.");
		hint.getStyleClass().add("subtle-text");

		view.getStyleClass().add("instrument-display");
		view.getChildren().addAll(header, canvasHolder, hint);

		animation = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (view.isVisible() && (dirty || playing)) {
					dirty = false;
					draw(now);
				}
			}
		};
		animation.start();
	}

	public Node getView() {
		return view;
	}

	public void setParts(List<InstrumentPart> parts, BarGrid barGrid, long songEndTick) {
		String previousName = selectedPart == null ? null : selectedPart.getName();
		this.parts = List.copyOf(parts);
		this.barGrid = barGrid;
		this.resolution = barGrid.getResolution();
		this.songEndTick = Math.max(0, songEndTick);
		reportedTick = 0;
		reportedAtNanos = System.nanoTime();
		ticksPerNano = 0;
		playing = false;
		partPicker.getItems().setAll(this.parts);
		partPicker.setDisable(this.parts.isEmpty());
		if (this.parts.isEmpty()) {
			partPicker.setValue(null);
			selectPart(null);
		} else {
			partPicker.setValue(this.parts.stream()
					.filter(part -> part.getName().equals(previousName))
					.findFirst()
					.orElse(this.parts.get(0)));
			selectPart(partPicker.getValue());
		}
		dirty = true;
	}

	public void updateTick(long tick) {
		updateTick(tick, false);
	}

	/** Receives the player position; while playing, motion between reports is interpolated. */
	public void updateTick(long tick, boolean isPlaying) {
		long now = System.nanoTime();
		long newTick = Math.max(0, tick);
		long elapsed = now - reportedAtNanos;
		boolean continuous = playing && isPlaying && newTick >= reportedTick
				&& elapsed > 0 && elapsed < 1_000_000_000L
				&& Math.abs(newTick - predictedTick(now)) < resolution;
		if (continuous) {
			double measured = (newTick - reportedTick) / (double) elapsed;
			ticksPerNano = ticksPerNano == 0 ? measured : ticksPerNano * 0.7 + measured * 0.3;
		} else if (!isPlaying) {
			ticksPerNano = 0;
		}
		reportedTick = newTick;
		reportedAtNanos = now;
		playing = isPlaying;
		dirty = true;
	}

	private long predictedTick(long now) {
		long elapsed = Math.min(Math.max(0, now - reportedAtNanos), MAX_EXTRAPOLATION_NANOS);
		return reportedTick + (playing ? Math.round(ticksPerNano * elapsed) : 0);
	}

	private void selectPart(InstrumentPart part) {
		selectedPart = part;
		rowByPitch.clear();
		if (part == null || part.getNotes().isEmpty()) {
			rowPitches = new int[0];
		} else if (part.isPercussion()) {
			TreeSet<Integer> used = new TreeSet<>();
			part.getNotes().forEach(note -> used.add(note.pitch()));
			rowPitches = used.descendingSet().stream().mapToInt(Integer::intValue).toArray();
		} else {
			int low = part.getLowestPitch();
			int high = part.getHighestPitch();
			int missing = MIN_PITCH_SPAN - (high - low + 1);
			if (missing > 0) {
				low -= missing / 2;
				high += missing - missing / 2;
			}
			low = Math.max(0, low - 1);
			high = Math.min(127, high + 1);
			rowPitches = new int[high - low + 1];
			for (int row = 0; row < rowPitches.length; row++) {
				rowPitches[row] = high - row;
			}
		}
		for (int row = 0; row < rowPitches.length; row++) {
			rowByPitch.put(rowPitches[row], row);
		}
		shownPitches = null;
		dirty = true;
	}

	private void draw(long now) {
		GraphicsContext g = canvas.getGraphicsContext2D();
		double width = canvas.getWidth();
		double height = canvas.getHeight();
		g.setFill(BACKGROUND);
		g.fillRect(0, 0, width, height);
		if (width <= 0 || height <= 0) {
			return;
		}
		if (selectedPart == null || rowPitches.length == 0) {
			drawCenteredMessage(g, width, height, parts.isEmpty()
					? "No instrument note events found in this song."
					: "Choose an instrument part to see its notes.");
			updateNowPlaying(List.of());
			return;
		}

		boolean percussion = selectedPart.isPercussion();
		double axisWidth = percussion ? DRUM_LABEL_WIDTH : KEYBOARD_WIDTH;
		double rollLeft = axisWidth;
		double rollWidth = Math.max(1, width - rollLeft);
		double rowHeight = Math.min(MAX_ROW_HEIGHT, (height - HEADER_HEIGHT) / rowPitches.length);
		double rollTop = HEADER_HEIGHT;
		double rollBottom = rollTop + rowHeight * rowPitches.length;

		double tick = Math.min(predictedTick(now), Math.max(songEndTick, reportedTick));
		double pixelsPerTick = rollWidth / (beatsVisible * resolution);
		double playheadX = rollLeft + rollWidth * PLAYHEAD_FRACTION;
		double firstVisibleTick = tick - (playheadX - rollLeft) / pixelsPerTick;
		double lastVisibleTick = tick + (width - playheadX) / pixelsPerTick;

		// Row backgrounds.
		for (int row = 0; row < rowPitches.length; row++) {
			int pitch = rowPitches[row];
			double y = rollTop + row * rowHeight;
			if (!percussion && isBlackKey(pitch)) {
				g.setFill(BLACK_KEY_ROW);
				g.fillRect(rollLeft, y, rollWidth, rowHeight);
			}
			if (percussion || Math.floorMod(pitch, 12) == 0) {
				g.setStroke(percussion ? GRID_BEAT : GRID_OCTAVE);
				g.setLineWidth(1);
				double lineY = Math.floor(y + rowHeight) + 0.5;
				g.strokeLine(rollLeft, lineY, width, lineY);
			}
		}

		// Header strip and bar/beat grid, following the song's time signatures.
		g.setFill(HEADER_BACKGROUND);
		g.fillRect(0, 0, width, HEADER_HEIGHT);
		g.setFont(SMALL_FONT);
		g.setTextBaseline(VPos.CENTER);
		g.setTextAlign(TextAlignment.LEFT);
		double lastLabelRight = Double.NEGATIVE_INFINITY;
		long fromTick = (long) Math.floor(Math.max(0, firstVisibleTick));
		long toTick = (long) Math.ceil(Math.max(0, lastVisibleTick));
		for (BarGrid.Line line : barGrid.lines(fromTick, toTick)) {
			BarGrid.Section section = barGrid.sectionAt(line.tick());
			boolean showBeats = section.beatTicks() * pixelsPerTick >= 10;
			if (!line.barStart() && !showBeats) {
				continue;
			}
			double x = Math.floor(playheadX + (line.tick() - tick) * pixelsPerTick) + 0.5;
			if (x < rollLeft) {
				continue;
			}
			g.setStroke(line.barStart() ? GRID_BAR : GRID_BEAT);
			g.strokeLine(x, line.barStart() ? 0 : rollTop, x, rollBottom);
			if (line.barStart() && x + 4 > lastLabelRight + 6) {
				boolean meterChange = line.tick() == section.tick() && line.tick() > 0;
				String label = line.bar() + (meterChange ? "  " + section.label().replace(" ", "") : "");
				g.setFill(TEXT_MUTED);
				g.fillText(label, x + 4, HEADER_HEIGHT / 2);
				lastLabelRight = x + 4 + label.length() * 6.5;
			}
		}

		// Notes.
		TreeSet<Integer> sounding = new TreeSet<>();
		g.setFont(LABEL_FONT);
		// Drum note lengths are arbitrary in MIDI, so hits are drawn as short fixed strikes.
		long hitTicks = Math.max(1, resolution / 4);
		for (NoteSpan note : selectedPart.getNotes()) {
			long endTick = percussion ? note.startTick() + hitTicks : note.endTick();
			boolean isSounding = note.startTick() <= tick && tick < endTick;
			if (isSounding) {
				sounding.add(note.pitch());
			}
			if (endTick < firstVisibleTick || note.startTick() > lastVisibleTick) {
				continue;
			}
			Integer row = rowByPitch.get(note.pitch());
			if (row == null) {
				continue;
			}
			double x1 = Math.max(rollLeft, playheadX + (note.startTick() - tick) * pixelsPerTick);
			double x2 = percussion
					? playheadX + (note.startTick() - tick) * pixelsPerTick + 10
					: playheadX + (endTick - tick) * pixelsPerTick;
			if (x2 <= rollLeft) {
				continue;
			}
			double noteWidth = Math.max(3, x2 - x1);
			double y = rollTop + row * rowHeight + 1;
			double noteHeight = Math.max(2, rowHeight - 2);
			boolean isPast = endTick <= tick;
			g.setFill(isSounding ? NOTE_SOUNDING_FILL : isPast ? NOTE_PAST_FILL : NOTE_FILL);
			g.setStroke(isSounding ? NOTE_SOUNDING_STROKE : isPast ? NOTE_PAST_STROKE : NOTE_STROKE);
			double arc = Math.min(6, noteHeight);
			g.fillRoundRect(x1, y, noteWidth, noteHeight, arc, arc);
			g.strokeRoundRect(x1 + 0.5, y + 0.5, noteWidth - 1, noteHeight - 1, arc, arc);
			if (!percussion && noteWidth >= 34 && rowHeight >= 14) {
				g.setFill(TEXT_DARK);
				g.setTextAlign(TextAlignment.LEFT);
				g.fillText(InstrumentPart.formatNote(note.pitch()), x1 + 4, y + noteHeight / 2);
			}
		}

		// Playhead.
		g.setStroke(PLAYHEAD);
		g.setLineWidth(2);
		g.strokeLine(playheadX, 0, playheadX, rollBottom);
		g.setLineWidth(1);

		// Axis drawn last so it stays fixed on the left.
		if (percussion) {
			drawDrumLabels(g, axisWidth, rowHeight, rollTop, sounding);
		} else {
			drawKeyboard(g, axisWidth, rowHeight, rollTop, sounding);
		}
		g.setFill(HEADER_BACKGROUND);
		g.fillRect(0, 0, axisWidth, HEADER_HEIGHT);
		g.setFill(TEXT_MUTED);
		g.setFont(SMALL_FONT);
		g.setTextAlign(TextAlignment.LEFT);
		g.fillText("Bar", 8, HEADER_HEIGHT / 2);

		updateNowPlaying(new ArrayList<>(sounding));
	}

	private void drawKeyboard(GraphicsContext g, double axisWidth, double rowHeight, double top,
			TreeSet<Integer> sounding) {
		g.setFont(LABEL_FONT);
		g.setTextBaseline(VPos.CENTER);
		for (int row = 0; row < rowPitches.length; row++) {
			int pitch = rowPitches[row];
			double y = top + row * rowHeight;
			boolean black = isBlackKey(pitch);
			boolean lit = sounding.contains(pitch);
			g.setFill(lit ? NOTE_SOUNDING_FILL : black ? BLACK_KEY : WHITE_KEY);
			double keyWidth = black ? axisWidth * 0.62 : axisWidth;
			g.fillRect(0, y, keyWidth, rowHeight);
			g.setStroke(KEY_BORDER);
			g.strokeRect(0.5, Math.floor(y) + 0.5, keyWidth - 1, rowHeight);
			boolean octave = Math.floorMod(pitch, 12) == 0;
			boolean labelled = lit || octave || rowHeight >= 16 && !black;
			if (labelled && (rowHeight >= 9 || octave && rowHeight >= 5)) {
				g.setFont(rowHeight >= 12 ? LABEL_FONT : SMALL_FONT);
				g.setFill(lit ? TEXT_DARK : black ? WHITE_KEY : TEXT_MUTED);
				g.setTextAlign(TextAlignment.RIGHT);
				g.fillText(InstrumentPart.formatNote(pitch), axisWidth - 6, y + rowHeight / 2);
			}
		}
		g.setStroke(GRID_BAR);
		g.strokeLine(axisWidth - 0.5, 0, axisWidth - 0.5, top + rowHeight * rowPitches.length);
	}

	private void drawDrumLabels(GraphicsContext g, double axisWidth, double rowHeight, double top,
			TreeSet<Integer> sounding) {
		g.setFont(LABEL_FONT);
		g.setTextBaseline(VPos.CENTER);
		g.setTextAlign(TextAlignment.LEFT);
		for (int row = 0; row < rowPitches.length; row++) {
			int pitch = rowPitches[row];
			double y = top + row * rowHeight;
			boolean lit = sounding.contains(pitch);
			g.setFill(lit ? NOTE_SOUNDING_FILL : row % 2 == 0 ? WHITE_KEY : BLACK_KEY_ROW);
			g.fillRect(0, y, axisWidth, rowHeight);
			if (rowHeight >= 9) {
				g.setFill(lit ? TEXT_DARK : TEXT_MUTED);
				g.fillText(drumName(pitch), 8, y + rowHeight / 2, axisWidth - 12);
			}
		}
		g.setStroke(GRID_BAR);
		g.strokeLine(axisWidth - 0.5, 0, axisWidth - 0.5, top + rowHeight * rowPitches.length);
	}

	private void drawCenteredMessage(GraphicsContext g, double width, double height, String message) {
		g.setFill(TEXT_MUTED);
		g.setFont(EMPTY_FONT);
		g.setTextAlign(TextAlignment.CENTER);
		g.setTextBaseline(VPos.CENTER);
		g.fillText(message, width / 2, height / 2);
	}

	private void updateNowPlaying(List<Integer> pitches) {
		if (pitches.equals(shownPitches)) {
			return;
		}
		shownPitches = pitches;
		boolean percussion = selectedPart != null && selectedPart.isPercussion();
		List<Node> chips = new ArrayList<>();
		for (int pitch : pitches) {
			Label chip = new Label(percussion ? drumName(pitch) : InstrumentPart.formatNote(pitch));
			chip.getStyleClass().add("note-chip");
			chips.add(chip);
		}
		if (chips.isEmpty()) {
			Label rest = new Label("—");
			rest.getStyleClass().add("subtle-text");
			chips.add(rest);
		}
		nowPlaying.getChildren().setAll(chips);
	}

	static String drumName(int pitch) {
		int index = pitch - FIRST_GM_DRUM;
		return index >= 0 && index < GM_DRUMS.length ? GM_DRUMS[index] : "Drum " + pitch;
	}

	private static boolean isBlackKey(int pitch) {
		return switch (Math.floorMod(pitch, 12)) {
			case 1, 3, 6, 8, 10 -> true;
			default -> false;
		};
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
