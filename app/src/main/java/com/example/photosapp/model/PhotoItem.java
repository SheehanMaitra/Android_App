package com.example.photosapp.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PhotoItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String uriString;
    private final String displayName;
    private final List<PhotoTag> tags = new ArrayList<>();

    public PhotoItem(String uriString, String displayName) {
        this.id = UUID.randomUUID().toString();
        this.uriString = Objects.requireNonNull(uriString, "uriString");
        this.displayName = displayName == null ? "" : displayName.trim();
    }

    public String getId() {
        return id;
    }

    public String getUriString() {
        return uriString;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<PhotoTag> getTags() {
        return tags;
    }
}

