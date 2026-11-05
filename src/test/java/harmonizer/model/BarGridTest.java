package harmonizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import org.junit.jupiter.api.Test;

class BarGridTest {
	private static final int RES = 480;

	@Test
	void assumesFourFourWhenFileHasNoTimeSignature() throws Exception {
		BarGrid grid = BarGrid.fromSequence(new Sequence(Sequence.PPQ, RES));
		assertTrue(grid.isAssumed());
		assertEquals("4 / 4 (assumed)", grid.describe());
		assertEquals(1, grid.barAt(0));
		assertEquals(2, grid.barAt(4 * RES));
	}

	@Test
	void compoundMeterUsesDottedQuarterBeats() throws Exception {
		BarGrid grid = BarGrid.fromSequence(withSignatures(new long[] {0}, new int[][] {{6, 3}}));
		assertEquals("6 / 8", grid.describe());
		assertFalse(grid.isAssumed());
		// A 6/8 bar is three quarter notes long, with two dotted-quarter beats.
		List<BarGrid.Line> lines = grid.lines(0, 3 * RES);
		assertEquals(List.of(
				new BarGrid.Line(0, true, 1),
				new BarGrid.Line(720, false, 1),
				new BarGrid.Line(1440, true, 2)), lines);
		assertEquals(3, grid.barAt(6 * RES));
	}

	@Test
	void followsTimeSignatureChanges() throws Exception {
		// Two bars of 4/4, then 3/4.
		BarGrid grid = BarGrid.fromSequence(
				withSignatures(new long[] {0, 8 * RES}, new int[][] {{4, 2}, {3, 2}}));
		assertEquals("4 / 4 (changes)", grid.describe());
		assertEquals("Bar 1: 4 / 4\nBar 3: 3 / 4", grid.describeChanges());
		assertEquals(2, grid.barAt(8 * RES - 1));
		assertEquals(3, grid.barAt(8 * RES));
		assertEquals(4, grid.barAt(11 * RES));
		assertTrue(grid.lines(11 * RES, 11 * RES).get(0).barStart());
	}

	private static Sequence withSignatures(long[] ticks, int[][] signatures) throws Exception {
		Sequence sequence = new Sequence(Sequence.PPQ, RES);
		Track track = sequence.createTrack();
		for (int index = 0; index < ticks.length; index++) {
			byte[] data = {(byte) signatures[index][0], (byte) signatures[index][1], 24, 8};
			MetaMessage message = new MetaMessage();
			message.setMessage(0x58, data, data.length);
			track.add(new MidiEvent(message, ticks[index]));
		}
		return sequence;
	}
}
