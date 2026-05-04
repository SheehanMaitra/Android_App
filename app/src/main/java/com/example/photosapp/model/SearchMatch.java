package com.example.photosapp.model;

public final class SearchMatch {

    private final String albumId;
    private final String albumName;
    private final int photoIndex;
    private final PhotoItem photo;

    public SearchMatch(String albumId, String albumName, int photoIndex, PhotoItem photo) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.photoIndex = photoIndex;
        this.photo = photo;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getPhotoIndex() {
        return photoIndex;
    }

    public PhotoItem getPhoto() {
        return photo;
    }
}

