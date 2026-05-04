package photos.model;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents one physical photo on disk.
 * Identity is determined solely by the absolute file path, so if the same file
 * is added to multiple albums, all albums share a single {@code Photo} instance,
 * and any caption or tag changes are visible everywhere.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class Photo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Absolute file-system path to the image file. Immutable after construction. */
    private final String absolutePath;

    /** User-supplied description, empty string if none. */
    private String caption = "";

    /** Last-modified time of the file, used as the "date taken". */
    private Instant dateTaken;

    /** Set of (type, value) tag pairs; insertion order preserved. */
    private final Set<Tag> tags = new LinkedHashSet<>();

    /**
     * Constructs a new Photo referencing the given file.
     *
     * @param absolutePath path to the image file; converted to an absolute path
     * @throws NullPointerException if {@code absolutePath} is null
     */
    public Photo(Path absolutePath) {
        Objects.requireNonNull(absolutePath, "absolutePath");
        this.absolutePath = absolutePath.toAbsolutePath().toString();
    }

    /**
     * Returns the absolute file-system path of this photo.
     *
     * @return absolute path string
     */
    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * Returns the user-supplied caption, or an empty string if none has been set.
     *
     * @return caption string (never {@code null})
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the caption. A {@code null} value is stored as an empty string.
     *
     * @param caption new caption
     */
    public void setCaption(String caption) {
        this.caption = caption == null ? "" : caption;
    }

    /**
     * Returns the capture date, derived from the file's last-modified time.
     *
     * @return date taken, or {@code null} if not yet set
     */
    public Instant getDateTaken() {
        return dateTaken;
    }

    /**
     * Sets the capture date (typically the file's last-modified timestamp).
     *
     * @param dateTaken instant representing when the photo was taken
     */
    public void setDateTaken(Instant dateTaken) {
        this.dateTaken = dateTaken;
    }

    /**
     * Returns the mutable set of tags associated with this photo.
     * Callers may add or remove tags directly.
     *
     * @return set of tags (insertion-ordered)
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * Two photos are equal if and only if they reference the same absolute file path.
     *
     * @param o object to compare
     * @return {@code true} if both photos point to the same file
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo photo)) return false;
        return absolutePath.equals(photo.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absolutePath);
    }
}
