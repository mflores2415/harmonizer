package harmonizer.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import harmonizer.player.MusicPlayer;

public final class Clock {
	static final int SLIDER_MAXIMUM = 10_000;

	private final JPanel component;
	private final JSlider timeline;
	private final JLabel elapsedTime;
	private final JLabel totalTime;
	private final Timer timer;
	private MusicPlayer musicPlayer;
	private boolean updatingTimeline;

	public Clock() {
		component = new JPanel(new BorderLayout(8, 0));
		component.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		elapsedTime = createTimeLabel();
		totalTime = createTimeLabel();
		timeline = new JSlider(0, SLIDER_MAXIMUM, 0);
		timeline.setEnabled(false);
		timeline.setFocusable(true);
		timeline.getAccessibleContext().setAccessibleName("Song position");
		timeline.getAccessibleContext().setAccessibleDescription("Seek within the current MIDI song");
		timeline.addChangeListener(event -> seekWhenAdjustmentFinishes());

		component.add(elapsedTime, BorderLayout.WEST);
		component.add(timeline, BorderLayout.CENTER);
		component.add(totalTime, BorderLayout.EAST);

		timer = new Timer(100, event -> updatePosition());
	}

	private JLabel createTimeLabel() {
		JLabel label = new JLabel("00:00");
		label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		return label;
	}

	public JPanel getComponent() {
		return component;
	}

	public void setMusicPlayer(MusicPlayer musicPlayer) {
		this.musicPlayer = musicPlayer;
	}

	public void createClock() {
		timer.stop();
		long length = musicPlayer.getDurationMicroseconds();
		updatingTimeline = true;
		timeline.setValue(0);
		timeline.setEnabled(length > 0);
		elapsedTime.setText(formatTime(0));
		totalTime.setText(formatTime(length));
		updatingTimeline = false;
	}

	public void play() {
		if (musicPlayer != null && musicPlayer.getDurationMicroseconds() > 0) {
			timer.start();
		}
	}

	public void pause() {
		timer.stop();
		updatePosition();
	}

	public void replay() {
		updatingTimeline = true;
		timeline.setValue(0);
		elapsedTime.setText(formatTime(0));
		updatingTimeline = false;
		if (musicPlayer != null && musicPlayer.getDurationMicroseconds() > 0) {
			timer.start();
		}
	}

	private void seekWhenAdjustmentFinishes() {
		if (updatingTimeline || timeline.getValueIsAdjusting() || musicPlayer == null) {
			return;
		}
		long targetPosition = toMicroseconds(timeline.getValue(), musicPlayer.getDurationMicroseconds());
		musicPlayer.seekToMicroseconds(targetPosition);
		elapsedTime.setText(formatTime(targetPosition));
	}

	private void updatePosition() {
		if (musicPlayer == null) {
			return;
		}
		long length = musicPlayer.getDurationMicroseconds();
		long position = Math.min(musicPlayer.getPositionMicroseconds(), length);
		if (!timeline.getValueIsAdjusting()) {
			updatingTimeline = true;
			timeline.setValue(length == 0 ? 0
					: (int) Math.round(position * (double) SLIDER_MAXIMUM / length));
			updatingTimeline = false;
		}
		elapsedTime.setText(formatTime(position));
		if (length > 0 && position >= length) {
			timer.stop();
			musicPlayer.isStopped(true);
		}
	}

	public static String formatTime(long microseconds) {
		long totalSeconds = Math.max(0, microseconds) / 1_000_000;
		return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
	}

	static long toMicroseconds(int sliderValue, long durationMicroseconds) {
		int boundedValue = Math.max(0, Math.min(sliderValue, SLIDER_MAXIMUM));
		return Math.round(Math.max(0, durationMicroseconds) * (boundedValue / (double) SLIDER_MAXIMUM));
	}
}
