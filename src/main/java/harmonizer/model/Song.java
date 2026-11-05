package harmonizer.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public class Song {
	public static final int MAX_TRANSPOSE = 12;
	private final NavigableMap<Long, Chord> chords = new TreeMap<>();
	private final Map<Long, String> chordOverrides = new HashMap<>();
	private final String fileName;
	private final Sequence sourceSequence;
	private List<InstrumentPart> instrumentParts;
	private final BarGrid barGrid;
	private Sequence transposedSequence;
	private int transposeSemitones;
	private Sequence sequence;
	private Track lyricTrack;
	private List<Long> ticks = List.of();
	private String name;
	private int tempo;
	private String keySignature;
	private boolean keyFlat;
	private boolean keyEstimated;
	private ChordAnalysisMode analysisMode;

	public Song(Path midiFile) throws IOException, InvalidMidiDataException {
		this(midiFile, ChordAnalysisMode.DETAILED);
	}

	public Song(Path midiFile, ChordAnalysisMode analysisMode) throws IOException, InvalidMidiDataException {
		fileName = midiFile.getFileName().toString();
		sourceSequence = MidiSystem.getSequence(midiFile.toFile());
		transposedSequence = sourceSequence;
		barGrid = BarGrid.fromSequence(sourceSequence);
		instrumentParts = InstrumentPart.fromSequence(sourceSequence);
		setAnalysisMode(analysisMode);
	}

	public void setAnalysisMode(ChordAnalysisMode analysisMode) throws InvalidMidiDataException {
		Objects.requireNonNull(analysisMode, "analysisMode");
		if (this.analysisMode == analysisMode) {
			return;
		}
		analyze(analysisMode);
	}

	/**
	 * Moves every pitched note of the song by {@code semitones} relative to the original file
	 * (drums are unaffected). This changes playback, the detected chords and what gets saved.
	 * Corrected chord names are cleared because they refer to the old chords.
	 */
	public void setTranspose(int semitones) throws InvalidMidiDataException {
		int bounded = Math.max(-MAX_TRANSPOSE, Math.min(MAX_TRANSPOSE, semitones));
		if (bounded == transposeSemitones) {
			return;
		}
		transposedSequence = MidiSongAnalyzer.transposeSequence(sourceSequence, bounded);
		transposeSemitones = bounded;
		instrumentParts = InstrumentPart.fromSequence(transposedSequence);
		chordOverrides.clear();
		analyze(analysisMode);
	}

	public int getTranspose() {
		return transposeSemitones;
	}

	/** Number of chord names the user has corrected by hand. */
	public int getCorrectedChordCount() {
		return chordOverrides.size();
	}

	private void analyze(ChordAnalysisMode analysisMode) throws InvalidMidiDataException {
		MidiSongAnalyzer.Analysis analysis = MidiSongAnalyzer.analyze(
				MidiSongAnalyzer.copySequence(transposedSequence),
				analysisMode,
				chordOverrides);
		this.analysisMode = analysisMode;
		sequence = analysis.sequence();
		chords.clear();
		chords.putAll(analysis.chords());
		ticks = List.copyOf(analysis.chordTicks());
		name = analysis.name();
		tempo = analysis.tempo();
		keySignature = analysis.keySignature();
		keyFlat = analysis.flatKey();
		keyEstimated = analysis.keyEstimated();
		lyricTrack = sequence.getTracks()[sequence.getTracks().length - 1];
	}

	public ChordAnalysisMode getAnalysisMode() {
		return analysisMode;
	}

	public String getName() {
		return name;
	}

	/** The time signature for display, e.g. "6 / 8", "4 / 4 (changes)" or "4 / 4 (assumed)". */
	public String getTimeSignature() {
		return barGrid.describe();
	}

	/** Bars and beats of the song, following its time signatures. */
	public BarGrid getBarGrid() {
		return barGrid;
	}

	public int getTempo() {
		return tempo;
	}

	public String getKey() {
		return keySignature;
	}

	/**
	 * True when the key was estimated from the notes because the file had no key signature,
	 * or its key signature didn't fit the music.
	 */
	public boolean isKeyEstimated() {
		return keyEstimated;
	}

	public void setKey(String key) {
		keySignature = key;
	}

	public String getFileName() {
		return fileName;
	}

	public List<InstrumentPart> getInstrumentParts() {
		return instrumentParts;
	}

	public void editChord(
			long tick,
			String newChords,
			String oldChords,
			long prevTick,
			String oldPrevChords,
			String prevChords) {
		Chord chord = chords.get(tick);
		if (chord != null) {
			String editedName = newChords.split(" ", 2)[0];
			chords.put(tick, chord.withName(editedName));
			chordOverrides.put(tick, editedName);
		}
		replaceLyric(tick, oldChords, newChords);
		if (prevTick != -1) {
			replaceLyric(prevTick, oldPrevChords, prevChords);
		}
	}

	private void replaceLyric(long tick, String oldText, String newText) {
		for (int index = lyricTrack.size() - 1; index >= 0; index--) {
			MidiEvent event = lyricTrack.get(index);
			if (event.getTick() != tick || !(event.getMessage() instanceof MetaMessage message)
					|| message.getType() != 5) {
				continue;
			}
			String eventText = new String(message.getData(), StandardCharsets.UTF_8);
			if (eventText.equals(oldText)) {
				lyricTrack.remove(event);
				break;
			}
		}
		lyricTrack.add(new MidiEvent(makeMessageSingle(5, newText), tick));
	}

	public Set<Chord> getChords() {
		return new HashSet<>(chords.values());
	}

	public Sequence getSequence() {
		return sequence;
	}

	private MetaMessage makeMessageSingle(int type, String value) {
		byte[] data = value.getBytes(StandardCharsets.UTF_8);
		try {
			MetaMessage message = new MetaMessage();
			message.setMessage(type, data, data.length);
			return message;
		} catch (InvalidMidiDataException exception) {
			throw new IllegalArgumentException("Unable to create MIDI lyric.", exception);
		}
	}

	/**
	 * Returns the tick of the chord playing at {@code tick}: the closest chord
	 * start at or before it, or the first chord if {@code tick} is before all of them.
	 */
	public long getChordTickAt(long tick) {
		Long key = chords.floorKey(tick);
		return key != null ? key : getFirstTick();
	}

	public long getFirstTick() {
		return ticks.isEmpty() ? -1L : ticks.get(0);
	}

	public long getPrevTick(long currentTick) {
		int index = ticks.indexOf(currentTick);
		return index > 0 ? ticks.get(index - 1) : -1L;
	}

	public long getNextTick(long currentTick) {
		int index = ticks.indexOf(currentTick);
		return index >= 0 && index < ticks.size() - 1 ? ticks.get(index + 1) : -1L;
	}

	public boolean isFlatKey() {
		return keyFlat;
	}

	public Map<Long, Chord> getMap() {
		return Collections.unmodifiableNavigableMap(chords);
	}
}
