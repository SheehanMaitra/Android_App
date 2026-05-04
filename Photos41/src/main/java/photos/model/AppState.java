package photos.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton holding application-wide session state shared across controllers.
 * Cleared on logout.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class AppState {

    private AppState() {}

    private static User currentUser;
    private static Album currentAlbum;
    private static List<Photo> searchResults = new ArrayList<>();

    /**
     * Returns the currently logged-in user, or {@code null} if no session is active.
     *
     * @return current user
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the currently logged-in user.
     *
     * @param user user object loaded from disk
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Returns the album currently open in the album view.
     *
     * @return current album, or {@code null}
     */
    public static Album getCurrentAlbum() {
        return currentAlbum;
    }

    /**
     * Sets the album to display in the album view.
     *
     * @param album album selected by the user
     */
    public static void setCurrentAlbum(Album album) {
        currentAlbum = album;
    }

    /**
     * Returns the most recent search result set.
     *
     * @return list of matching photos (never {@code null})
     */
    public static List<Photo> getSearchResults() {
        return searchResults;
    }

    /**
     * Stores search results so the search controller can create an album from them.
     *
     * @param results photos matching the search criteria
     */
    public static void setSearchResults(List<Photo> results) {
        searchResults = results == null ? new ArrayList<>() : results;
    }

    /** Resets all session state. Called on logout. */
    public static void clear() {
        currentUser = null;
        currentAlbum = null;
        searchResults = new ArrayList<>();
    }
}
