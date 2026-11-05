package harmonizer.model;

/**
 * The key shown for a song.
 *
 * @param name      e.g. "G major", or "Unknown" for a song without pitched notes
 * @param flat      whether chord names should be spelled with flats
 * @param estimated true when the key was estimated from the notes instead of read from the file
 */
public record SongKey(String name, boolean flat, boolean estimated) {
	public static final SongKey UNKNOWN = new SongKey("Unknown", false, false);
}
