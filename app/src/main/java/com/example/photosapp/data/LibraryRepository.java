package com.example.photosapp.data;

import android.content.Context;

import com.example.photosapp.model.Album;
import com.example.photosapp.model.PhotoItem;
import com.example.photosapp.model.PhotoLibrary;
import com.example.photosapp.model.PhotoTag;
import com.example.photosapp.model.SearchMatch;
import com.example.photosapp.model.TagType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class LibraryRepository {

    private static final String STORAGE_FILE = "photos_library.ser";

    private static LibraryRepository instance;

    private final Context appContext;
    private PhotoLibrary library;

    private LibraryRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.library = loadLibrary();
    }

    public static synchronized LibraryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LibraryRepository(context);
        }
        return instance;
    }

    public synchronized List<Album> getAlbums() {
        return Collections.unmodifiableList(library.getAlbums());
    }

    public synchronized Album createAlbum(String name) {
        validateAlbumName(name, null);
        Album album = new Album(name);
        library.getAlbums().add(album);
        saveLibrary();
        return album;
    }

    public synchronized void renameAlbum(String albumId, String newName) {
        Album album = requireAlbum(albumId);
        validateAlbumName(newName, albumId);
        album.setName(newName);
        saveLibrary();
    }

    public synchronized void deleteAlbum(String albumId) {
        Album album = requireAlbum(albumId);
        library.getAlbums().remove(album);
        saveLibrary();
    }

    public synchronized Album getAlbum(String albumId) {
        return requireAlbum(albumId);
    }

    public synchronized PhotoItem getPhoto(String albumId, String photoId) {
        return requirePhoto(requireAlbum(albumId), photoId);
    }

    public synchronized int getPhotoIndex(String albumId, String photoId) {
        Album album = requireAlbum(albumId);
        for (int i = 0; i < album.getPhotos().size(); i++) {
            if (album.getPhotos().get(i).getId().equals(photoId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Photo not found.");
    }

    public synchronized void addPhotoToAlbum(String albumId, String uriString, String displayName) {
        Album album = requireAlbum(albumId);
        String normalizedUri = normalizeText(uriString);
        if (normalizedUri.isEmpty()) {
            throw new IllegalArgumentException("Photo URI is missing.");
        }
        if (albumContainsUri(album, normalizedUri)) {
            throw new IllegalArgumentException("That photo is already in this album.");
        }
        album.getPhotos().add(new PhotoItem(normalizedUri, normalizeDisplayName(displayName, normalizedUri)));
        saveLibrary();
    }

    public synchronized void removePhoto(String albumId, String photoId) {
        Album album = requireAlbum(albumId);
        PhotoItem photo = requirePhoto(album, photoId);
        album.getPhotos().remove(photo);
        saveLibrary();
    }

    public synchronized void movePhoto(String sourceAlbumId, String targetAlbumId, String photoId) {
        if (Objects.equals(sourceAlbumId, targetAlbumId)) {
            throw new IllegalArgumentException("Pick a different target album.");
        }
        Album sourceAlbum = requireAlbum(sourceAlbumId);
        Album targetAlbum = requireAlbum(targetAlbumId);
        PhotoItem photo = requirePhoto(sourceAlbum, photoId);
        if (albumContainsUri(targetAlbum, photo.getUriString())) {
            throw new IllegalArgumentException("That album already contains this photo.");
        }
        sourceAlbum.getPhotos().remove(photo);
        targetAlbum.getPhotos().add(photo);
        saveLibrary();
    }

    public synchronized void addTag(String albumId, String photoId, TagType type, String value) {
        PhotoItem photo = requirePhoto(requireAlbum(albumId), photoId);
        String normalizedValue = normalizeText(value);
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Tag value cannot be empty.");
        }
        if (type == TagType.LOCATION && hasTagType(photo, TagType.LOCATION)) {
            throw new IllegalArgumentException("Only one location tag is allowed per photo.");
        }
        PhotoTag newTag = new PhotoTag(type, normalizedValue);
        if (photo.getTags().contains(newTag)) {
            throw new IllegalArgumentException("That tag already exists on this photo.");
        }
        photo.getTags().add(newTag);
        saveLibrary();
    }

    public synchronized void deleteTag(String albumId, String photoId, int tagIndex) {
        PhotoItem photo = requirePhoto(requireAlbum(albumId), photoId);
        if (tagIndex < 0 || tagIndex >= photo.getTags().size()) {
            throw new IllegalArgumentException("Select a tag to delete.");
        }
        photo.getTags().remove(tagIndex);
        saveLibrary();
    }

    public synchronized List<String> getAutocompleteValues(TagType type, String prefix) {
        String normalizedPrefix = normalizeText(prefix).toLowerCase(Locale.US);
        Set<String> suggestions = new LinkedHashSet<>();
        for (Album album : library.getAlbums()) {
            for (PhotoItem photo : album.getPhotos()) {
                for (PhotoTag tag : photo.getTags()) {
                    if (tag.getType() == type) {
                        if (normalizedPrefix.isEmpty()
                                || tag.getValue().toLowerCase(Locale.US).startsWith(normalizedPrefix)) {
                            suggestions.add(tag.getValue());
                        }
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(suggestions);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    public synchronized List<SearchMatch> search(
            TagType firstType,
            String firstValue,
            TagType secondType,
            String secondValue,
            boolean matchAll
    ) {
        String normalizedFirstValue = normalizeText(firstValue);
        if (normalizedFirstValue.isEmpty()) {
            throw new IllegalArgumentException("The first tag value is required.");
        }

        boolean useSecond = secondType != null && !normalizeText(secondValue).isEmpty();
        String normalizedSecondValue = normalizeText(secondValue);

        List<SearchMatch> matches = new ArrayList<>();
        for (Album album : library.getAlbums()) {
            List<PhotoItem> photos = album.getPhotos();
            for (int i = 0; i < photos.size(); i++) {
                PhotoItem photo = photos.get(i);
                boolean firstMatches = photoMatches(photo, firstType, normalizedFirstValue);
                boolean overall = firstMatches;
                if (useSecond) {
                    boolean secondMatches = photoMatches(photo, secondType, normalizedSecondValue);
                    overall = matchAll ? firstMatches && secondMatches : firstMatches || secondMatches;
                }
                if (overall) {
                    matches.add(new SearchMatch(album.getId(), album.getName(), i, photo));
                }
            }
        }
        matches.sort(Comparator.comparing(SearchMatch::getAlbumName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(match -> match.getPhoto().getDisplayName(), String.CASE_INSENSITIVE_ORDER));
        return matches;
    }

    private boolean photoMatches(PhotoItem photo, TagType type, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.US);
        for (PhotoTag tag : photo.getTags()) {
            if (tag.getType() == type && tag.matchesPrefix(normalizedPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTagType(PhotoItem photo, TagType type) {
        for (PhotoTag tag : photo.getTags()) {
            if (tag.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private boolean albumContainsUri(Album album, String uriString) {
        for (PhotoItem photo : album.getPhotos()) {
            if (photo.getUriString().equals(uriString)) {
                return true;
            }
        }
        return false;
    }

    private void validateAlbumName(String name, String ignoreAlbumId) {
        String normalized = normalizeText(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Album name cannot be empty.");
        }
        for (Album album : library.getAlbums()) {
            if (!album.getId().equals(ignoreAlbumId)
                    && album.getName().equalsIgnoreCase(normalized)) {
                throw new IllegalArgumentException("An album with that name already exists.");
            }
        }
    }

    private Album requireAlbum(String albumId) {
        for (Album album : library.getAlbums()) {
            if (album.getId().equals(albumId)) {
                return album;
            }
        }
        throw new IllegalArgumentException("Album not found.");
    }

    private PhotoItem requirePhoto(Album album, String photoId) {
        for (PhotoItem photo : album.getPhotos()) {
            if (photo.getId().equals(photoId)) {
                return photo;
            }
        }
        throw new IllegalArgumentException("Photo not found.");
    }

    private void saveLibrary() {
        File file = new File(appContext.getFilesDir(), STORAGE_FILE);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(library);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save library.", exception);
        }
    }

    private PhotoLibrary loadLibrary() {
        File file = new File(appContext.getFilesDir(), STORAGE_FILE);
        if (!file.exists()) {
            return new PhotoLibrary();
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object loaded = input.readObject();
            if (loaded instanceof PhotoLibrary photoLibrary) {
                return photoLibrary;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // Fall back to a fresh library if the saved file is missing or invalid.
        }
        return new PhotoLibrary();
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeDisplayName(String displayName, String fallbackUri) {
        String normalized = normalizeText(displayName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        int slash = fallbackUri.lastIndexOf('/');
        return slash >= 0 && slash < fallbackUri.length() - 1
                ? fallbackUri.substring(slash + 1)
                : fallbackUri;
    }
}

