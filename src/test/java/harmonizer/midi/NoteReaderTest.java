package harmonizer.midi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.junit.jupiter.api.Test;

class NoteReaderTest {
	@Test
	void pairsNoteOnsWithNoteOffsInOrderAndTracksTheProgram() throws Exception {
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		Track track = sequence.createTrack();
		add(track, ShortMessage.PROGRAM_CHANGE, 0, 56, 0, 0);
		add(track, ShortMessage.NOTE_ON, 0, 60, 100, 0);
		add(track, ShortMessage.NOTE_ON, 0, 60, 100, 100);
		add(track, ShortMessage.NOTE_ON, 0, 60, 0, 200); // velocity 0 = note off
		add(track, ShortMessage.NOTE_OFF, 0, 60, 0, 300);

		assertEquals(List.of(
				new PlayedNote(0, 0, 56, 60, 0, 200),
				new PlayedNote(0, 0, 56, 60, 100, 300)), NoteReader.read(sequence));
	}

	@Test
	void endsUnfinishedNotesAtTheEndOfTheirTrack() throws Exception {
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		Track track = sequence.createTrack();
		add(track, ShortMessage.NOTE_ON, Midi.DRUM_CHANNEL, 38, 100, 10);
		add(track, ShortMessage.NOTE_ON, 1, 64, 100, 20);
		add(track, ShortMessage.NOTE_OFF, 1, 64, 0, 960);

		List<PlayedNote> notes = NoteReader.read(sequence);

		assertEquals(2, notes.size());
		PlayedNote drum = notes.get(0);
		assertEquals(true, drum.isDrum());
		assertEquals(track.ticks(), drum.endTick());
	}

	private static void add(Track track, int command, int channel, int data1, int data2, long tick) throws Exception {
		ShortMessage message = new ShortMessage();
		message.setMessage(command, channel, data1, data2);
		track.add(new MidiEvent(message, tick));
	}
}
