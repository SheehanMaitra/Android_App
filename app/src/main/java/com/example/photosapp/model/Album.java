package com.example.photosapp.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Album implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private final List<PhotoItem> photos = new ArrayList<>();

    public Album(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = normalizeName(name);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public List<PhotoItem> getPhotos() {
        return photos;
    }

    public int getPhotoCount() {
        return photos.size();
    }

    private static String normalizeName(String name) {
        String trimmed = Objects.requireNonNull(name, "name").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Album name cannot be empty.");
        }
        return trimmed;
    }
}

