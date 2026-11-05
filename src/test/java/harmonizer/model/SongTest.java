package harmonizer.model;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SongTest {
	@TempDir
	Path tempDir;

	@Test
	void importsMidiFromDirectoryWithoutTrailingSeparator() throws Exception {
		Path midiFile = createChordMidi("chord.mid");

		Song song = Song.load(midiFile);

		assertTrue(song.getChordsByTick().values().stream().anyMatch(chord -> chord.getName().equals("Cmaj")));
		assertEquals(-1, song.getNextTick(song.getFirstTick()));
		assertTrue(hasLyric(song.getSequence(), song.getFirstTick(), "Cmaj END"));
	}

	@Test
	void importsKaraokeMidiAndPreservesItsLyrics() throws Exception {
		Path karaokeFile = tempDir.resolve("song.kar");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addMeta(track, 5, "Hello world".getBytes(StandardCharsets.UTF_8), 0);
		addTriad(track, 60, 64, 67, 0, 480);
		MidiSystem.write(sequence, 1, karaokeFile.toFile());

		Song song = Song.load(karaokeFile);

		assertTrue(song.getChordsByTick().values().stream().anyMatch(chord -> chord.getName().equals("Cmaj")));
		assertTrue(hasLyric(song.getSequence(), 0, "Hello world"));
		assertTrue(hasLyric(song.getSequence(), song.getFirstTick(), "Cmaj END"));
	}

	@Test
	void editingChordUpdatesModelAndMidiLyric() throws Exception {
		Path midiFile = createChordMidi("editable-chord.mid");
		Song song = Song.load(midiFile);
		long chordTick = song.getChordsByTick().keySet().iterator().next();

		song.renameChord(chordTick, "Dmaj");

		assertEquals("Dmaj", song.getChordsByTick().get(chordTick).getName());
		assertTrue(song.getUniqueChords().stream().anyMatch(chord -> chord.getName().equals("Dmaj")));
		assertTrue(hasLyric(song.getSequence(), chordTick, "Dmaj END"));
	}

	@Test
	void emptySongHasNoChordTicks() throws Exception {
		Path midiFile = tempDir.resolve("empty.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		sequence.createTrack();
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);

		assertTrue(song.getChordsByTick().isEmpty());
		assertEquals(-1, song.getFirstTick());
		assertEquals(-1, song.getChordTickAt(0));
		assertEquals(-1, song.getPrevTick(0));
		assertEquals(-1, song.getNextTick(0));
	}

	@Test
	void chordMapCannotBeModifiedOutsideSong() throws Exception {
		Song song = Song.load(createChordMidi("read-only-chords.mid"));
		var chord = song.getChordsByTick().values().iterator().next();

		assertThrows(UnsupportedOperationException.class, () -> song.getChordsByTick().put(100L, chord));
	}

	@Test
	void chordNavigationHandlesTicksOutsideTheSong() throws Exception {
		Song song = Song.load(createChordMidi("chord-navigation.mid"));
		long firstTick = song.getFirstTick();

		assertEquals(firstTick, song.getChordTickAt(firstTick - 1));
		assertEquals(-1, song.getPrevTick(firstTick));
		assertEquals(-1, song.getNextTick(Long.MAX_VALUE));
	}

	@Test
	void importsMidiMetadataAndUsesItsKeyForChordSpelling() throws Exception {
		Path midiFile = tempDir.resolve("metadata.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addMeta(track, 3, "Metadata Test".getBytes(StandardCharsets.UTF_8), 0);
		addMeta(track, 0x51, new byte[] { 0x07, (byte) 0xA1, 0x20 }, 0);
		addMeta(track, 0x58, new byte[] { 4, 2, 24, 8 }, 0);
		addMeta(track, 0x59, new byte[] { -5, 0 }, 0);
		addNote(track, ShortMessage.NOTE_ON, 61, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 65, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 68, 100, 0);
		addNote(track, ShortMessage.NOTE_OFF, 61, 0, 480);
		addNote(track, ShortMessage.NOTE_OFF, 65, 0, 480);
		addNote(track, ShortMessage.NOTE_OFF, 68, 0, 480);
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);

		assertEquals("Metadata Test", song.getName());
		assertEquals(120, song.getTempo());
		assertEquals("4 / 4", song.getTimeSignature());
		assertEquals("Db major", song.getKey().name());
		assertTrue(song.getKey().flat());
		assertTrue(song.getChordsByTick().values().stream().anyMatch(chord -> chord.getName().equals("Dbmaj")));
		assertTrue(hasLyric(song.getSequence(), song.getFirstTick(), "Dbmaj END"));
	}

	@Test
	void chordLyricsShowCurrentAndUpcomingChordsAtTheirStartTicks() throws Exception {
		Path midiFile = tempDir.resolve("chord-transition.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addTriad(track, 60, 64, 67, 0, 480);
		addTriad(track, 62, 65, 69, 480, 960);
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);
		long firstChordTick = song.getFirstTick();
		long secondChordTick = song.getNextTick(firstChordTick);

		assertEquals("Cmaj", song.getChordsByTick().get(firstChordTick).getName());
		assertEquals("Dmin", song.getChordsByTick().get(secondChordTick).getName());
		assertTrue(hasLyric(song.getSequence(), firstChordTick, "Cmaj Dmin"));
		assertTrue(hasLyric(song.getSequence(), secondChordTick, "Dmin END"));
	}

	@Test
	void steadyModeKeepsTheFirstQualityUntilTheRootChanges() throws Exception {
		Path midiFile = createSameRootQualityChangeMidi();

		Song detailed = Song.load(midiFile, ChordAnalysisMode.DETAILED);
		Song steady = Song.load(midiFile, ChordAnalysisMode.STEADY);

		assertEquals(2, detailed.getChordsByTick().size());
		assertEquals("Cmaj", detailed.getChordsByTick().values().iterator().next().getName());
		assertEquals("Cmin", detailed.getChordsByTick().values().stream().skip(1).findFirst().orElseThrow().getName());
		assertEquals(1, steady.getChordsByTick().size());
		assertEquals("Cmaj", steady.getChordsByTick().values().iterator().next().getName());
		assertTrue(hasLyric(steady.getSequence(), steady.getFirstTick(), "Cmaj END"));
	}

	@Test
	void switchingAnalysisModeKeepsChordEdits() throws Exception {
		Song song = Song.load(createSameRootQualityChangeMidi(), ChordAnalysisMode.DETAILED);
		long tick = song.getFirstTick();

		song.renameChord(tick, "Dmaj");
		song.setAnalysisMode(ChordAnalysisMode.STEADY);
		assertEquals("Dmaj", song.getChordsByTick().get(tick).getName());
		assertTrue(hasLyric(song.getSequence(), tick, "Dmaj END"));

		song.setAnalysisMode(ChordAnalysisMode.DETAILED);
		assertEquals("Dmaj", song.getChordsByTick().get(tick).getName());
	}

	@Test
	void steadyModeRemovesGeneratedLyricsForFilteredChordChanges() throws Exception {
		Song detailed = Song.load(createSameRootQualityChangeMidi(), ChordAnalysisMode.DETAILED);
		long filteredChordTick = detailed.getNextTick(detailed.getFirstTick());
		Path analyzedFile = tempDir.resolve("analyzed-chords.mid");
		MidiSystem.write(detailed.getSequence(), 1, analyzedFile.toFile());

		Song steady = Song.load(analyzedFile, ChordAnalysisMode.STEADY);

		assertEquals(1, steady.getChordsByTick().size());
		assertTrue(hasLyric(steady.getSequence(), steady.getFirstTick(), "Cmaj END"));
		assertFalse(hasLyric(steady.getSequence(), filteredChordTick, "Cmin END"));
	}

	@Test
	void identifiesChordFromNotesAcrossTracksAndKeepsItWhileNotesOverlap() throws Exception {
		Path midiFile = tempDir.resolve("overlapping-tracks.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var firstTrack = sequence.createTrack();
		var secondTrack = sequence.createTrack();
		addNote(firstTrack, ShortMessage.NOTE_ON, 60, 100, 0);
		addNote(firstTrack, ShortMessage.NOTE_OFF, 60, 0, 480);
		addNote(secondTrack, ShortMessage.NOTE_ON, 64, 100, 0);
		addNote(secondTrack, ShortMessage.NOTE_ON, 67, 100, 0);
		addNote(secondTrack, ShortMessage.NOTE_OFF, 64, 0, 960);
		addNote(secondTrack, ShortMessage.NOTE_OFF, 67, 0, 960);
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);

		assertEquals(1, song.getChordsByTick().size());
		assertEquals("Cmaj", song.getChordsByTick().values().iterator().next().getName());
		assertEquals(0, song.getChordsByTick().keySet().iterator().next());
	}

	@Test
	void listsInstrumentPartsAndNotesAtRequestedTicks() throws Exception {
		Path midiFile = tempDir.resolve("trumpet-part.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addMeta(track, 3, "Lead".getBytes(StandardCharsets.UTF_8), 0);
		ShortMessage programChange = new ShortMessage();
		programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 56, 0);
		track.add(new MidiEvent(programChange, 0));
		addTriad(track, 60, 64, 67, 0, 480);
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);

		assertEquals(1, song.getInstrumentParts().size());
		InstrumentPart trumpet = song.getInstrumentParts().get(0);
		assertTrue(trumpet.getName().contains("Trumpet"));
		assertEquals(List.of(60, 64, 67), trumpet.getNotesAt(0));
		assertEquals(List.of(60, 64, 67), trumpet.getNotesAt(240));
		assertEquals(List.of(), trumpet.getNotesAt(480));
		assertTrue(trumpet.getNotes().stream()
				.allMatch(note -> note.startTick() == 0 && note.endTick() == 480));
		assertEquals("C4", Pitch.withOctave(60));
	}

	@Test
	void transposingMovesNotesChordsAndKeyButNotDrums() throws Exception {
		Path midiFile = tempDir.resolve("transpose.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addMeta(track, 0x59, new byte[] {0, 0}, 0);
		addTriad(track, 60, 64, 67, 0, 480);
		ShortMessage drumOn = new ShortMessage();
		drumOn.setMessage(ShortMessage.NOTE_ON, 9, 38, 100);
		track.add(new MidiEvent(drumOn, 0));
		ShortMessage drumOff = new ShortMessage();
		drumOff.setMessage(ShortMessage.NOTE_OFF, 9, 38, 0);
		track.add(new MidiEvent(drumOff, 120));
		MidiSystem.write(sequence, 1, midiFile.toFile());

		Song song = Song.load(midiFile);
		long chordTick = song.getFirstTick();
		song.renameChord(chordTick, "Amin");
		assertEquals(1, song.getCorrectedChordCount());

		song.setTranspose(2);

		assertEquals(2, song.getTranspose());
		assertEquals("D major", song.getKey().name());
		assertEquals(0, song.getCorrectedChordCount());
		assertEquals("Dmaj", song.getChordsByTick().get(song.getFirstTick()).getName());
		InstrumentPart pitched = song.getInstrumentParts().stream()
				.filter(part -> !part.isPercussion()).findFirst().orElseThrow();
		InstrumentPart drums = song.getInstrumentParts().stream()
				.filter(InstrumentPart::isPercussion).findFirst().orElseThrow();
		assertEquals(List.of(62, 66, 69), pitched.getNotesAt(0));
		assertEquals(List.of(38), drums.getNotesAt(0));

		song.setTranspose(-1);
		assertEquals("B major", song.getKey().name());
		assertEquals("Bmaj", song.getChordsByTick().get(song.getFirstTick()).getName());

		song.setTranspose(0);
		assertEquals("C major", song.getKey().name());
		assertEquals("Cmaj", song.getChordsByTick().get(song.getFirstTick()).getName());
	}

	private Path createChordMidi(String fileName) throws Exception {
		Path midiFile = tempDir.resolve(fileName);
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addNote(track, ShortMessage.NOTE_ON, 60, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 64, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 67, 100, 0);
		addNote(track, ShortMessage.NOTE_ON, 60, 0, 480);
		addNote(track, ShortMessage.NOTE_ON, 64, 0, 480);
		addNote(track, ShortMessage.NOTE_ON, 67, 0, 480);
		MidiSystem.write(sequence, 1, midiFile.toFile());
		return midiFile;
	}

	private Path createSameRootQualityChangeMidi() throws Exception {
		Path midiFile = tempDir.resolve("same-root-quality-change.mid");
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		var track = sequence.createTrack();
		addTriad(track, 60, 64, 67, 0, 480);
		addTriad(track, 60, 63, 67, 480, 960);
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

	private void addMeta(javax.sound.midi.Track track, int type, byte[] data, long tick) throws Exception {
		MetaMessage message = new MetaMessage();
		message.setMessage(type, data, data.length);
		track.add(new MidiEvent(message, tick));
	}

	private void addTriad(
			javax.sound.midi.Track track,
			int root,
			int third,
			int fifth,
			long startTick,
			long endTick) throws Exception {
		addNote(track, ShortMessage.NOTE_ON, root, 100, startTick);
		addNote(track, ShortMessage.NOTE_ON, third, 100, startTick);
		addNote(track, ShortMessage.NOTE_ON, fifth, 100, startTick);
		addNote(track, ShortMessage.NOTE_OFF, root, 0, endTick);
		addNote(track, ShortMessage.NOTE_OFF, third, 0, endTick);
		addNote(track, ShortMessage.NOTE_OFF, fifth, 0, endTick);
	}

	private void addNote(javax.sound.midi.Track track, int command, int pitch, int velocity, long tick)
			throws Exception {
		ShortMessage message = new ShortMessage();
		message.setMessage(command, 0, pitch, velocity);
		track.add(new MidiEvent(message, tick));
	}
}
