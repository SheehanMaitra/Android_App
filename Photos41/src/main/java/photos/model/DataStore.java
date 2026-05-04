package photos.model;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Handles all persistence for the Photos application using Java serialization
 * ({@link ObjectOutputStream} / {@link ObjectInputStream}).
 *
 * <p>Directory layout under the working directory:
 * <pre>
 *   data/
 *     users.dat        – serialized List&lt;String&gt; of registered usernames
 *     tagtypes.dat     – serialized Map&lt;String,Boolean&gt; of tag-type → allowsMultiple
 *     users/
 *       &lt;username&gt;.dat – serialized {@link User} object
 *     stock/stock/     – image files for the built-in stock album
 * </pre>
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class DataStore {

    private DataStore() {}

    // ── Path constants ────────────────────────────────────────────────

    /** Root data directory (relative to working directory). */
    private static final Path DATA_DIR = Path.of("data");

    /** Directory holding per-user serialized files. */
    public static final Path USERS_DIR = DATA_DIR.resolve("users");

    /** File holding the list of registered usernames. */
    private static final Path USERS_LIST_FILE = DATA_DIR.resolve("users.dat");

    /** File holding the tag-type registry. */
    private static final Path TAG_TYPES_FILE = DATA_DIR.resolve("tagtypes.dat");

    /** Directory containing stock image files shipped with the application. */
    private static final Path STOCK_PHOTO_DIR =
            DATA_DIR.resolve("stock").resolve("stock");

    private static final String STOCK_USERNAME  = "stock";
    private static final String STOCK_ALBUM_NAME = "stock";

    /** Supported image file extensions (lower-case). */
    private static final Set<String> IMAGE_EXTS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp");

    /** Default tag types shipped with the application. */
    private static final Map<String, Boolean> DEFAULT_TAG_TYPES;
    static {
        DEFAULT_TAG_TYPES = new LinkedHashMap<>();
        DEFAULT_TAG_TYPES.put("location", false); // single value per photo
        DEFAULT_TAG_TYPES.put("person",   true);  // multiple values per photo
    }

    // ── Low-level I/O ─────────────────────────────────────────────────

    /**
     * Serializes {@code object} to {@code file}, creating parent directories as needed.
     *
     * @param file   destination path
     * @param object object to serialize
     * @throws IOException if an I/O error occurs
     */
    public static void save(Path file, Serializable object) throws IOException {
        Files.createDirectories(file.getParent());
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            oos.writeObject(object);
        }
    }

    /**
     * Deserializes an object of type {@code T} from {@code file}.
     *
     * @param <T>  expected type
     * @param file source path
     * @param type class token for casting
     * @return deserialized object
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the serialized class is not on the classpath
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(Path file, Class<T> type)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            return type.cast(ois.readObject());
        }
    }

    // ── User list ─────────────────────────────────────────────────────

    /**
     * Returns the list of registered usernames, or an empty list on first run.
     *
     * @return mutable list of usernames
     */
    @SuppressWarnings("unchecked")
    public static List<String> loadUsernames() {
        if (!Files.exists(USERS_LIST_FILE)) return new ArrayList<>();
        try {
            return load(USERS_LIST_FILE, ArrayList.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Persists the current list of registered usernames.
     *
     * @param usernames list to save
     * @throws IOException if an I/O error occurs
     */
    public static void saveUsernames(List<String> usernames) throws IOException {
        save(USERS_LIST_FILE, new ArrayList<>(usernames));
    }

    // ── Per-user data ─────────────────────────────────────────────────

    /**
     * Loads the {@link User} object for the given username from disk.
     * Returns a new empty {@code User} if no file exists yet.
     *
     * @param username user to load
     * @return deserialized user, or a fresh empty user
     */
    public static User loadUser(String username) {
        Path file = USERS_DIR.resolve(username + ".dat");
        if (!Files.exists(file)) return new User(username);
        try {
            return load(file, User.class);
        } catch (Exception e) {
            return new User(username);
        }
    }

    /**
     * Serializes the user's current state (albums + photos + tags) to disk.
     *
     * @param user user to persist
     * @throws IOException if an I/O error occurs
     */
    public static void saveUser(User user) throws IOException {
        Path file = USERS_DIR.resolve(user.getUsername() + ".dat");
        save(file, user);
    }

    /**
     * Removes the persisted file for the given username, if it exists.
     *
     * @param username user whose file should be deleted
     */
    public static void deleteUserFile(String username) {
        try {
            Files.deleteIfExists(USERS_DIR.resolve(username + ".dat"));
        } catch (IOException ignored) {}
    }

    // ── Tag-type registry ─────────────────────────────────────────────

    /**
     * Loads the tag-type registry (name → allowsMultiple).
     * Returns the default registry if no file exists yet.
     *
     * @return mutable map of tag types
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> loadTagTypes() {
        if (!Files.exists(TAG_TYPES_FILE)) return new LinkedHashMap<>(DEFAULT_TAG_TYPES);
        try {
            return load(TAG_TYPES_FILE, LinkedHashMap.class);
        } catch (Exception e) {
            return new LinkedHashMap<>(DEFAULT_TAG_TYPES);
        }
    }

    /**
     * Persists the tag-type registry.
     *
     * @param tagTypes map to save
     * @throws IOException if an I/O error occurs
     */
    public static void saveTagTypes(Map<String, Boolean> tagTypes) throws IOException {
        save(TAG_TYPES_FILE, new LinkedHashMap<>(tagTypes));
    }

    // ── Stock initialization ──────────────────────────────────────────

    /**
     * Creates the stock user and album from image files in {@code data/stock/stock/}
     * if the stock user has not yet been initialized. Safe to call on every startup.
     */
    public static void initStockUserIfNeeded() {
        try {
            Files.createDirectories(USERS_DIR);
            Files.createDirectories(STOCK_PHOTO_DIR);
        } catch (IOException ignored) {}

        ensureStockUsernamePresent();

        Path stockFile = USERS_DIR.resolve(STOCK_USERNAME + ".dat");
        if (Files.exists(stockFile)) return; // already initialized

        User stockUser = new User(STOCK_USERNAME);
        Album stockAlbum = new Album(STOCK_ALBUM_NAME);
        stockUser.getAlbums().add(stockAlbum);

        if (Files.isDirectory(STOCK_PHOTO_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    STOCK_PHOTO_DIR, DataStore::isImageFile)) {
                for (Path imgPath : stream) {
                    Photo photo = new Photo(imgPath.toAbsolutePath());
                    try {
                        Instant mtime = Files.getLastModifiedTime(imgPath).toInstant();
                        photo.setDateTaken(mtime);
                    } catch (IOException ignored) {}
                    stockAlbum.getPhotos().add(photo);
                }
            } catch (IOException ignored) {}
        }

        try {
            saveUser(stockUser);
        } catch (IOException ignored) {}
    }

    /** Returns {@code true} if the path has a recognised image extension. */
    private static boolean isImageFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return IMAGE_EXTS.stream().anyMatch(name::endsWith);
    }

    /** Ensures the stock username is present in the registered-user list. */
    private static void ensureStockUsernamePresent() {
        try {
            List<String> names = loadUsernames();
            boolean hasStock = names.stream().anyMatch(name -> name.equalsIgnoreCase(STOCK_USERNAME));
            if (!hasStock) {
                names.add(STOCK_USERNAME);
                saveUsernames(names);
            }
        } catch (IOException ignored) {}
    }
}
