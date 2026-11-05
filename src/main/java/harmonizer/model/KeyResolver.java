package harmonizer.model;

import java.util.List;
import java.util.Optional;

import harmonizer.midi.PlayedNote;

/**
 * Decides which key to show for a song. The file's key signature is trusted when it fits the
 * notes; otherwise (missing, or a poor fit such as a default C major) the key is estimated with
 * {@link KeyDetector}. A key signature can't tell a major key from its relative minor, so the
 * better-fitting of the two is used.
 */
final class KeyResolver {
	// A declared key fitting worse than this is treated as unreliable (e.g. a default C major).
	private static final double POOR_FIT = 0.5;
	// An estimate must fit at least this well before it overrides a declared key.
	private static final double CONFIDENT_FIT = 0.7;
	// The relative major/minor must fit this much better before the mode is switched.
	private static final double MODE_MARGIN = 0.1;

	private KeyResolver() {
	}

	static SongKey resolve(List<PlayedNote> notes, Optional<KeySignature> declaredSignature) {
		double[] histogram = KeyDetector.pitchClassDurations(notes);
		Optional<KeyDetector.Key> estimate = KeyDetector.detect(histogram);
		if (declaredSignature.isEmpty()) {
			return estimate.map(KeyResolver::estimated).orElse(SongKey.UNKNOWN);
		}
		KeySignature declared = declaredSignature.get();
		if (estimate.isEmpty()) {
			return fromFile(declared, false);
		}

		boolean modeSwitched = false;
		double declaredFit = fit(histogram, declared);
		double relativeFit = fit(histogram, declared.relative());
		if (relativeFit > declaredFit + MODE_MARGIN) {
			declared = declared.relative();
			declaredFit = relativeFit;
			modeSwitched = true;
		}
		if (declaredFit < POOR_FIT && estimate.get().score() >= CONFIDENT_FIT) {
			return estimated(estimate.get());
		}
		return fromFile(declared, modeSwitched);
	}

	private static double fit(double[] histogram, KeySignature signature) {
		return KeyDetector.score(histogram, signature.tonic(), signature.minor());
	}

	private static SongKey fromFile(KeySignature signature, boolean modeSwitched) {
		return new SongKey(signature.name(), signature.flat(), modeSwitched);
	}

	private static SongKey estimated(KeyDetector.Key key) {
		return new SongKey(key.name(), key.flat(), true);
	}
}
