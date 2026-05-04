package photos.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single application user and all of their albums.
 * Serialized to disk by {@link DataStore} at the end of each session.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Unique, case-sensitive login name. */
    private final String username;

    /** All albums owned by this user. */
    private final List<Album> albums = new ArrayList<>();

    /**
     * Constructs a new user with the given username.
     *
     * @param username login name (trimmed); must not be null
     */
    public User(String username) {
        this.username = Objects.requireNonNull(username, "username").trim();
    }

    /**
     * Returns this user's login name.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the mutable list of albums owned by this user.
     * Callers may add or remove albums directly.
     *
     * @return album list
     */
    public List<Album> getAlbums() {
        return albums;
    }
}
