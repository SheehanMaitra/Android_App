package com.example.photosapp.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class PhotoLibrary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Album> albums = new ArrayList<>();

    public List<Album> getAlbums() {
        return albums;
    }
}

