package com.example.photosapp.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

public enum TagType implements Serializable {
    PERSON("person", "Person"),
    LOCATION("location", "Location");

    @Serial
    private static final long serialVersionUID = 1L;

    private final String storageValue;
    private final String displayName;

    TagType(String storageValue, String displayName) {
        this.storageValue = storageValue;
        this.displayName = displayName;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TagType fromDisplayName(String value) {
        if (value == null) {
            return PERSON;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        for (TagType type : values()) {
            if (type.displayName.toLowerCase(Locale.US).equals(normalized)
                    || type.storageValue.equals(normalized)) {
                return type;
            }
        }
        return PERSON;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

