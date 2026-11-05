package harmonizer.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import harmonizer.midi.Midi;

/**
 * A loaded song and its analysis. The original sequence is never changed: transposing and chord
 * name corrections produce a new playable {@link #getSequence() sequence} each time.
 */
public final class Song {
	public static final int MAX_TRANSPOSE = 12;

	private final String fileName;
	private final Sequence original;
	private final BarGrid barGrid;
	private final Map<Long, String> chordNameCorrections = new HashMap<>();
	private ChordAnalysisMode analysisMode;
	private int transposeSemitones;
	private Sequence transposed;
	private MidiSongAnalyzer.Analysis analysis;

	private Song(String fileName, Sequence original, ChordAnalysisMode analysisMode)
			throws InvalidMidiDataException {
		this.fileName = Objects.requireNonNull(fileName, "fileName");
		this.original = Objects.requireNonNull(original, "original");
		this.analysisMode = Objects.requireNonNull(analysisMode, "analysisMode");
		barGrid = BarGrid.fromSequence(original);
		transposed = original;
		analyze();
	}

	public static Song load(Path midiFile) throws IOException, InvalidMidiDataException {
		return load(midiFile, ChordAnalysisMode.DETAILED);
	}

	public static Song load(Path midiFile, ChordAnalysisMode analysisMode)
			throws IOException, InvalidMidiDataException {
		return of(midiFile.getFileName().toString(), MidiSystem.getSequence(midiFile.toFile()), analysisMode);
	}

	/** A song from a sequence already in memory. */
	public static Song of(String fileName, Sequence sequence, ChordAnalysisMode analysisMode)
			throws InvalidMidiDataException {
		return new Song(fileName, sequence, analysisMode);
	}

	public void setAnalysisMode(ChordAnalysisMode analysisMode) throws InvalidMidiDataException {
		Objects.requireNonNull(analysisMode, "analysisMode");
		if (this.analysisMode != analysisMode) {
			this.analysisMode = analysisMode;
			analyze();
		}
	}

	public ChordAnalysisMode getAnalysisMode() {
		return analysisMode;
	}

	/**
	 * Moves every pitched note of the song by {@code semitones} relative to the original file
	 * (drums are unaffected). This changes playback, the detected chords and what gets saved.
	 * Corrected chord names are cleared because they refer to the old chords.
	 */
	public void setTranspose(int semitones) throws InvalidMidiDataException {
		int bounded = Math.clamp(semitones, -MAX_TRANSPOSE, MAX_TRANSPOSE);
		if (bounded == transposeSemitones) {
			return;
		}
		transposed = Transposer.transpose(original, bounded);
		transposeSemitones = bounded;
		chordNameCorrections.clear();
		analyze();
	}

	public int getTranspose() {
		return transposeSemitones;
	}

	/**
	 * Gives the chord starting at {@code tick} a corrected name. Only the name shown and saved
	 * with the file changes; the music does not.
	 */
	public void renameChord(long tick, String name) throws InvalidMidiDataException {
		if (!analysis.chords().containsKey(tick)) {
			throw new IllegalArgumentException("No chord starts at tick " + tick);
		}
		chordNameCorrections.put(tick, Objects.requireNonNull(name, "name"));
		analyze();
	}

	/** Number of chord names the user has corrected by hand. */
	public int getCorrectedChordCount() {
		return chordNameCorrections.size();
	}

	private void analyze() throws InvalidMidiDataException {
		analysis = MidiSongAnalyzer.analyze(
				Midi.copy(transposed), analysisMode, chordNameCorrections);
	}

	public String getFileName() {
		return fileName;
	}

	/** The name stored in the file, or null. */
	public String getName() {
		return analysis.name();
	}

	/** Tempo in BPM from the file, or 0 if the file doesn't say. */
	public int getTempo() {
		return analysis.tempo();
	}

	public SongKey getKey() {
		return analysis.key();
	}

	/** The time signature for display, e.g. "6 / 8", "4 / 4 (changes)" or "4 / 4 (assumed)". */
	public String getTimeSignature() {
		return barGrid.describe();
	}

	/** Bars and beats of the song, following its time signatures. */
	public BarGrid getBarGrid() {
		return barGrid;
	}

	public List<InstrumentPart> getInstrumentParts() {
		return analysis.instrumentParts();
	}

	/** The sequence to play and save: transposed if requested, with chord lyrics added. */
	public Sequence getSequence() {
		return analysis.sequence();
	}

	/** Chords keyed by the tick where each one starts. */
	public NavigableMap<Long, Chord> getChordsByTick() {
		return Collections.unmodifiableNavigableMap(analysis.chords());
	}

	/** Each distinct chord the song uses. */
	public Set<Chord> getUniqueChords() {
		return new HashSet<>(analysis.chords().values());
	}

	public boolean hasChords() {
		return !analysis.chords().isEmpty();
	}

	/**
	 * The tick of the chord playing at {@code tick}: the closest chord start at or before it,
	 * or the first chord if {@code tick} is before all of them; -1 if there are no chords.
	 */
	public long getChordTickAt(long tick) {
		Long key = analysis.chords().floorKey(tick);
		return key != null ? key : getFirstTick();
	}

	/** The first chord's tick, or -1 if there are no chords. */
	public long getFirstTick() {
		return analysis.chords().isEmpty() ? -1L : analysis.chords().firstKey();
	}

	/** The tick of the chord before the one starting at {@code chordTick}, or -1. */
	public long getPrevTick(long chordTick) {
		return analysis.chords().containsKey(chordTick) ? orNone(analysis.chords().lowerKey(chordTick)) : -1L;
	}

	/** The tick of the chord after the one starting at {@code chordTick}, or -1. */
	public long getNextTick(long chordTick) {
		return analysis.chords().containsKey(chordTick) ? orNone(analysis.chords().higherKey(chordTick)) : -1L;
	}

	private static long orNone(Long tick) {
		return tick == null ? -1L : tick;
	}
}
