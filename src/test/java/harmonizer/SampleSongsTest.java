package harmonizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.Song;

/** Loads every song in the samples folder and checks what Harmonizer finds in it. */
class SampleSongsTest {
	private static final Path SAMPLES = Path.of("samples");

	static Stream<Arguments> samples() {
		return Stream.of(
				Arguments.of("encourage.mid", "G major", true, "4 / 4"),
				Arguments.of("ww.mid", "G minor", true, "4 / 4"),
				Arguments.of("wwiwiwo.mid", "G minor", true, "4 / 4"),
				Arguments.of("Movie_Themes_-_Ghostbusters.mid", "A major", true, "4 / 4"),
				Arguments.of("fg.mid", "F major", false, "12 / 8"),
				Arguments.of("march.mid", "C major", false, "6 / 8"),
				Arguments.of("drums.mid", "Unknown", false, "4 / 4 (assumed)"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("samples")
	void analyzesSampleSong(String file, String key, boolean keyEstimated, String timeSignature) throws Exception {
		Song song = Song.load(SAMPLES.resolve(file), ChordAnalysisMode.DETAILED);

		assertEquals(key, song.getKey().name());
		assertEquals(keyEstimated, song.getKey().estimated());
		assertEquals(timeSignature, song.getTimeSignature());
		assertFalse(song.getInstrumentParts().isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("samples")
	void everyPitchedSampleHasChords(String file, String key, boolean keyEstimated, String timeSignature)
			throws Exception {
		Song song = Song.load(SAMPLES.resolve(file));
		boolean hasPitchedNotes = song.getInstrumentParts().stream().anyMatch(part -> !part.isPercussion());
		assertEquals(hasPitchedNotes, song.hasChords());
	}

	@Test
	void samplesFolderHoldsOnlyMidiFiles() throws Exception {
		try (Stream<Path> files = Files.list(SAMPLES)) {
			assertTrue(files.allMatch(path -> path.getFileName().toString().matches(".*[.](mid|midi|kar)")));
		}
	}
}
