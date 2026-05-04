package com.example.photosapp.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public final class PhotoTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TagType type;
    private final String value;

    public PhotoTag(TagType type, String value) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = value == null ? "" : value.trim();
    }

    public TagType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean matchesPrefix(String prefix) {
        return value.toLowerCase(Locale.US).startsWith(prefix.toLowerCase(Locale.US));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PhotoTag photoTag)) {
            return false;
        }
        return type == photoTag.type
                && value.trim().equalsIgnoreCase(photoTag.value.trim());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value.trim().toLowerCase(Locale.US));
    }

    @Override
    public String toString() {
        return type.getDisplayName() + ": " + value;
    }
}

