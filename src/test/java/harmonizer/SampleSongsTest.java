package harmonizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import harmonizer.model.ChordAnalysisMode;
import harmonizer.model.Song;

/**
 * Analyzes real songs and checks what Harmonizer finds in them: the public-domain demos in
 * {@code samples/}, plus third-party files in the git-ignored {@code local-samples/} folder
 * when it exists on this machine (those tests are skipped elsewhere, e.g. on CI).
 */
class SampleSongsTest {
	private static final Path SAMPLES = Path.of("samples");
	private static final Path LOCAL_SAMPLES = Path.of("local-samples");

	/** file, title, key, key estimated?, time signature */
	static Stream<Arguments> songs() {
		return Stream.of(
				Arguments.of(SAMPLES.resolve("ode-to-joy.mid"), "Ode to Joy", "D major", false, "4 / 4"),
				Arguments.of(SAMPLES.resolve("greensleeves.mid"), "Greensleeves", "A minor", false, "3 / 4"),
				Arguments.of(LOCAL_SAMPLES.resolve("encourage.mid"), "*Encouragement", "G major", true, "4 / 4"),
				Arguments.of(LOCAL_SAMPLES.resolve("ww.mid"), "Who Walks in When I Walk Out", "G minor", true, "4 / 4"),
				Arguments.of(LOCAL_SAMPLES.resolve("wwiwiwo.mid"), "Who Walks in When I Walk Out", "G minor", true,
						"4 / 4"),
				Arguments.of(LOCAL_SAMPLES.resolve("Movie_Themes_-_Ghostbusters.mid"), "Ghost Busters Theme",
						"A major", true, "4 / 4"),
				Arguments.of(LOCAL_SAMPLES.resolve("fg.mid"), "Family guy theme song", "F major", false, "12 / 8"),
				Arguments.of(LOCAL_SAMPLES.resolve("march.mid"), "Monty Python Theme", "C major", false, "6 / 8"),
				Arguments.of(LOCAL_SAMPLES.resolve("drums.mid"), null, "Unknown", false, "4 / 4 (assumed)"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("songs")
	void analyzesSong(Path file, String title, String key, boolean keyEstimated, String timeSignature)
			throws Exception {
		Song song = load(file, ChordAnalysisMode.DETAILED);

		assertEquals(title, song.getName());
		assertEquals(key, song.getKey().name());
		assertEquals(keyEstimated, song.getKey().estimated());
		assertEquals(timeSignature, song.getTimeSignature());
		assertFalse(song.getInstrumentParts().isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("songs")
	void findsChordsWheneverThereArePitchedNotes(Path file) throws Exception {
		Song song = load(file, ChordAnalysisMode.STEADY);
		boolean hasPitchedNotes = song.getInstrumentParts().stream().anyMatch(part -> !part.isPercussion());
		assertEquals(hasPitchedNotes, song.hasChords());
	}

	@Test
	void demoSongsAreAlwaysPresent() {
		assertTrue(Files.isRegularFile(SAMPLES.resolve("ode-to-joy.mid")));
		assertTrue(Files.isRegularFile(SAMPLES.resolve("greensleeves.mid")));
	}

	@Test
	void samplesFolderHoldsOnlyMidiFiles() throws Exception {
		try (Stream<Path> files = Files.list(SAMPLES)) {
			assertTrue(files.allMatch(path -> path.getFileName().toString().matches(".*[.](mid|midi|kar)")));
		}
	}

	private static Song load(Path file, ChordAnalysisMode mode) throws Exception {
		assumeTrue(Files.exists(file), () -> file + " is not on this machine");
		return Song.load(file, mode);
	}
}
