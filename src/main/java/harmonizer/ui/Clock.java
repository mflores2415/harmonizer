package harmonizer.ui;

import java.util.Locale;
import java.util.function.Consumer;

import harmonizer.player.MusicPlayer;
import harmonizer.player.PlaybackUpdate;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public final class Clock {
	static final double SLIDER_MAXIMUM = 10_000;

	private final BorderPane view = new BorderPane();
	private final Slider timeline = new Slider(0, SLIDER_MAXIMUM, 0);
	private final Label elapsedTime = new Label("00:00");
	private final Label totalTime = new Label("00:00");
	private final Timeline refreshTimeline;
	private final MusicPlayer musicPlayer;
	private long durationMicroseconds;
	private boolean updatingSlider;
	private Consumer<Boolean> playingListener = playing -> { };

	public Clock(MusicPlayer musicPlayer) {
		this.musicPlayer = musicPlayer;
		timeline.setDisable(true);
		timeline.setMaxWidth(Double.MAX_VALUE);
		timeline.setAccessibleText("Song position");
		elapsedTime.getStyleClass().add("time-label");
		totalTime.getStyleClass().add("time-label");

		HBox controls = new HBox(12, elapsedTime, timeline, totalTime);
		controls.setAlignment(Pos.CENTER);
		controls.setPadding(new Insets(8, 12, 8, 12));
		HBox.setHgrow(timeline, javafx.scene.layout.Priority.ALWAYS);
		view.setCenter(controls);
		view.getStyleClass().add("timeline");

		timeline.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
			if (!isChanging) {
				seekToSliderPosition();
			}
		});
		timeline.valueProperty().addListener((observable, previous, current) -> {
			if (!updatingSlider && !timeline.isValueChanging()) {
				seekToSliderPosition();
			}
		});

		refreshTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> updateFromPlayer()));
		refreshTimeline.setCycleCount(Timeline.INDEFINITE);
	}

	public Node getView() {
		return view;
	}

	public void reset() {
		refreshTimeline.stop();
		durationMicroseconds = 0;
		updatingSlider = true;
		timeline.setValue(0);
		timeline.setDisable(true);
		updatingSlider = false;
		elapsedTime.setText(formatTime(0));
		totalTime.setText(formatTime(0));
		playingListener.accept(false);
	}

	public void setPlayingListener(Consumer<Boolean> playingListener) {
		this.playingListener = playingListener;
	}

	public void update(PlaybackUpdate update) {
		durationMicroseconds = update.durationMicroseconds();
		timeline.setDisable(durationMicroseconds <= 0);
		totalTime.setText(formatTime(durationMicroseconds));
		if (!timeline.isValueChanging()) {
			updatingSlider = true;
			timeline.setValue(toSliderValue(update.positionMicroseconds(), durationMicroseconds));
			updatingSlider = false;
		}
		elapsedTime.setText(formatTime(update.positionMicroseconds()));
		if (update.playing()) {
			playingListener.accept(true);
			if (refreshTimeline.getStatus() != Timeline.Status.RUNNING) {
				refreshTimeline.play();
			}
		} else {
			playingListener.accept(false);
			refreshTimeline.pause();
		}
	}

	private void updateFromPlayer() {
		update(musicPlayer.getPlaybackUpdate());
	}

	private void seekToSliderPosition() {
		if (updatingSlider || durationMicroseconds <= 0) {
			return;
		}
		long position = toMicroseconds(timeline.getValue(), durationMicroseconds);
		elapsedTime.setText(formatTime(position));
		musicPlayer.seekToMicroseconds(position);
	}

	public static String formatTime(long microseconds) {
		long totalSeconds = Math.max(0, microseconds) / 1_000_000;
		return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
	}

	static long toMicroseconds(double sliderValue, long durationMicroseconds) {
		double boundedValue = Math.max(0, Math.min(sliderValue, SLIDER_MAXIMUM));
		return Math.round(Math.max(0, durationMicroseconds) * (boundedValue / SLIDER_MAXIMUM));
	}

	static double toSliderValue(long positionMicroseconds, long durationMicroseconds) {
		if (durationMicroseconds <= 0) {
			return 0;
		}
		long boundedPosition = Math.max(0, Math.min(positionMicroseconds, durationMicroseconds));
		return boundedPosition * SLIDER_MAXIMUM / durationMicroseconds;
	}
}
