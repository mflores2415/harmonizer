package harmonizer;

import harmonizer.model.Song;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SongMidiTest {
	@TempDir
	Path tempDir;

	@Test
	void importsMidiFromDirectoryWithoutTrailingSeparator() throws Exception {
		Path midiFile = createChordMidi("chord.mid");

		Song song = new Song(midiFile);

		assertTrue(song.getMap().values().stream().anyMatch(chord -> chord.getName().equals("Cmaj")));
	}

	@Test
	void editingChordUpdatesModelAndMidiLyric() throws Exception {
		Path midiFile = createChordMidi("editable-chord.mid");
		Song song = new Song(midiFile);
		long chordTick = song.getMap().keySet().iterator().next();

		song.editChord(chordTick, "Dmaj END", "Cmaj END", -1, "", "");

		assertEquals("Dmaj", song.getMap().get(chordTick).getName());
		assertTrue(song.getChords().stream().anyMatch(chord -> chord.getName().equals("Dmaj")));
		assertTrue(hasLyric(song.getSequence(), chordTick, "Dmaj END"));
	}

	private Path createChordMidi(String fileName) throws Exception {
		Path midiFile = tempDir.resolve(fileName);
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addNote(track, ShortMessage.NOTE_ON, 60, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 64, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 67, 100, 0);
		addNote(track, ShortMessage.NOTE_OFF, 60, 0, 480);
		addNote(track, ShortMessage.NOTE_OFF, 64, 0, 480);
		addNote(track, ShortMessage.NOTE_OFF, 67, 0, 480);
		MidiSystem.write(sequence, 1, midiFile.toFile());
		return midiFile;
	}

	private boolean hasLyric(Sequence sequence, long tick, String expected) {
		for (var track : sequence.getTracks()) {
			for (int index = 0; index < track.size(); index++) {
				MidiEvent event = track.get(index);
				if (event.getTick() == tick && event.getMessage() instanceof MetaMessage message
						&& message.getType() == 5
						&& expected.equals(new String(message.getData(), StandardCharsets.UTF_8))) {
					return true;
				}
			}
		}
		return false;
	}

	private void addNote(javax.sound.midi.Track track, int command, int pitch, int velocity, long tick)
			throws Exception {
		ShortMessage message = new ShortMessage();
		message.setMessage(command, 0, pitch, velocity);
		track.add(new MidiEvent(message, tick));
	}
}
