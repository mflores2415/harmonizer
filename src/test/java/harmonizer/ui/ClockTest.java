package harmonizer.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClockTest {
	@Test
	void formatsElapsedTimeAsMinutesAndSeconds() {
		assertEquals("00:00", Clock.formatTime(0));
		assertEquals("01:05", Clock.formatTime(65_000_000));
		assertEquals("125:00", Clock.formatTime(7_500_000_000L));
	}

	@Test
	void formatsNegativePositionsAsZero() {
		assertEquals("00:00", Clock.formatTime(-1));
	}

	@Test
	void mapsTimelinePositionsToSongTime() {
		assertEquals(0, Clock.toMicroseconds(0, 60_000_000));
		assertEquals(30_000_000, Clock.toMicroseconds(Clock.SLIDER_MAXIMUM / 2, 60_000_000));
		assertEquals(60_000_000, Clock.toMicroseconds(Clock.SLIDER_MAXIMUM, 60_000_000));
	}

	@Test
	void clampsTimelinePositionsAndNegativeDurations() {
		assertEquals(0, Clock.toMicroseconds(-1, 60_000_000));
		assertEquals(60_000_000, Clock.toMicroseconds(Clock.SLIDER_MAXIMUM + 1, 60_000_000));
		assertEquals(0, Clock.toMicroseconds(Clock.SLIDER_MAXIMUM, -1));
	}
}
