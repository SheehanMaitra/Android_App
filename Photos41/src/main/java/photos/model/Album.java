package photos.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Represents a named album containing references to {@link Photo} objects.
 * Multiple albums may reference the same {@code Photo} instance.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class Album implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Album name shown in the user's album list. */
    private String name;

    /** Photos currently contained in this album. */
    private final List<Photo> photos = new ArrayList<>();

    /**
     * Constructs an empty album with the given name.
     *
     * @param name album name (trimmed); must not be null
     */
    public Album(String name) {
        this.name = Objects.requireNonNull(name, "name").trim();
    }

    /**
     * Returns the album name.
     *
     * @return album name
     */
    public String getName() {
        return name;
    }

    /**
     * Renames this album.
     *
     * @param name new name (trimmed); must not be null
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name").trim();
    }

    /**
     * Returns the mutable list of photos in this album.
     *
     * @return photo list
     */
    public List<Photo> getPhotos() {
        return photos;
    }

    /**
     * Returns the earliest {@link Photo#getDateTaken()} among all photos,
     * or {@code null} if the album is empty or no dates are set.
     *
     * @return earliest capture date, or {@code null}
     */
    public Instant getEarliestDate() {
        return photos.stream()
                .map(Photo::getDateTaken)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Returns the latest {@link Photo#getDateTaken()} among all photos,
     * or {@code null} if the album is empty or no dates are set.
     *
     * @return latest capture date, or {@code null}
     */
    public Instant getLatestDate() {
        return photos.stream()
                .map(Photo::getDateTaken)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public String toString() {
        return name;
    }
}
