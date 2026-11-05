package harmonizer.model;

import java.util.List;
import java.util.Optional;

import javax.sound.midi.Sequence;

import harmonizer.midi.NoteReader;
import harmonizer.midi.PlayedNote;

/**
 * Estimates a song's key with the Krumhansl–Schmuckler key-finding algorithm: build a
 * duration-weighted histogram of the 12 pitch classes, correlate it with the Krumhansl–Kessler
 * major and minor key profiles rotated to all 12 tonics, and pick the best-matching key.
 */
public final class KeyDetector {
	// Krumhansl & Kessler (1982) probe-tone ratings, starting from the tonic.
	private static final double[] MAJOR_PROFILE = {
			6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88
	};
	private static final double[] MINOR_PROFILE = {
			6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17
	};
	private static final String[] MAJOR_NAMES = {
			"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
	};
	private static final String[] MINOR_NAMES = {
			"C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"
	};

	private KeyDetector() {
	}

	/** A key with its correlation score (−1..1, higher means a better fit). */
	public record Key(int tonic, boolean minor, double score) {
		public String name() {
			return (minor ? MINOR_NAMES[tonic] + " minor" : MAJOR_NAMES[tonic] + " major");
		}

		/** True when the key's signature uses flats, so chords should be spelled with flats. */
		public boolean flat() {
			return minor
					? switch (tonic) { case 0, 2, 3, 5, 7, 10 -> true; default -> false; }
					: switch (tonic) { case 1, 3, 5, 8, 10 -> true; default -> false; };
		}
	}

	/** Estimates the key from the song's pitched notes, or empty if it has none. */
	public static Optional<Key> detect(Sequence sequence) {
		return detect(pitchClassDurations(NoteReader.read(sequence)));
	}

	static Optional<Key> detect(double[] histogram) {
		double total = 0;
		for (double value : histogram) {
			total += value;
		}
		if (total <= 0) {
			return Optional.empty();
		}
		Key best = null;
		for (int tonic = 0; tonic < 12; tonic++) {
			for (boolean minor : new boolean[] {false, true}) {
				double score = correlation(histogram, minor ? MINOR_PROFILE : MAJOR_PROFILE, tonic);
				if (best == null || score > best.score()) {
					best = new Key(tonic, minor, score);
				}
			}
		}
		return Optional.of(best);
	}

	/** How well a specific key fits the song's notes (same scale as {@link Key#score()}). */
	static double score(double[] histogram, int tonic, boolean minor) {
		return correlation(histogram, minor ? MINOR_PROFILE : MAJOR_PROFILE, tonic);
	}

	/** Total ticks each pitch class sounds, summed over all non-drum notes. */
	static double[] pitchClassDurations(List<PlayedNote> notes) {
		double[] durations = new double[12];
		for (PlayedNote note : notes) {
			if (!note.isDrum()) {
				durations[Pitch.pitchClass(note.pitch())] += note.duration();
			}
		}
		return durations;
	}

	private static double correlation(double[] histogram, double[] profile, int tonic) {
		double histogramMean = mean(histogram);
		double profileMean = mean(profile);
		double covariance = 0;
		double histogramVariance = 0;
		double profileVariance = 0;
		for (int pitchClass = 0; pitchClass < 12; pitchClass++) {
			double x = histogram[pitchClass] - histogramMean;
			double y = profile[Math.floorMod(pitchClass - tonic, 12)] - profileMean;
			covariance += x * y;
			histogramVariance += x * x;
			profileVariance += y * y;
		}
		double denominator = Math.sqrt(histogramVariance * profileVariance);
		return denominator == 0 ? 0 : covariance / denominator;
	}

	private static double mean(double[] values) {
		double sum = 0;
		for (double value : values) {
			sum += value;
		}
		return sum / values.length;
	}
}
