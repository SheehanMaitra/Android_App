package photos.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * An immutable (name, value) tag pair, e.g. {@code ("location", "New Brunswick")}.
 * Two tags are considered equal when both name and value match exactly.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class Tag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Tag type name, such as {@code location} or {@code person}. */
    private final String name;

    /** Tag value paired with the type name. */
    private final String value;

    /**
     * Constructs a tag with the given type name and value (both trimmed).
     *
     * @param name  tag type name (e.g. "location"); must not be null
     * @param value tag value (e.g. "Paris"); must not be null
     */
    public Tag(String name, String value) {
        this.name  = Objects.requireNonNull(name,  "name").trim();
        this.value = Objects.requireNonNull(value, "value").trim();
    }

    /**
     * Returns the tag type name.
     *
     * @return name, e.g. "location"
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the tag value.
     *
     * @return value, e.g. "New Brunswick"
     */
    public String getValue() {
        return value;
    }

    /**
     * Two tags are equal when both name and value are identical.
     *
     * @param o object to compare
     * @return {@code true} if both name and value match
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag tag)) return false;
        return name.equals(tag.name) && value.equals(tag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    /**
     * Returns a human-readable representation in {@code "name=value"} format.
     *
     * @return tag string
     */
    @Override
    public String toString() {
        return name + "=" + value;
    }
}
