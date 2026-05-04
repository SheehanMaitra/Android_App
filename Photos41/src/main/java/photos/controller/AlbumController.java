package photos.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import photos.Photos;
import photos.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the album detail screen.
 * Displays all photos in an album as thumbnail+caption list cells.
 * Selecting a photo shows its full image, caption, date, and tags on the right.
 * Supports add, remove, caption, tag management, copy/move, and slideshow navigation.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class AlbumController {

    @FXML private Label           albumTitleLabel;
    @FXML private ListView<Photo> photosListView;
    @FXML private ImageView       photoImageView;
    @FXML private Label           captionLabel;
    @FXML private Label           dateLabel;
    @FXML private Label           tagsLabel;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a").withZone(ZoneId.systemDefault());

    /** Creates the controller for the album-detail scene. */
    public AlbumController() {}

    // ── Initialisation ───────────────────────────────────────────────

    /**
     * Loads and displays the current album from {@link AppState}.
     * Sets up the thumbnail cell factory and the selection listener.
     * Called by {@link AlbumsController} after the scene is loaded.
     */
    public void init() {
        Album album = AppState.getCurrentAlbum();
        albumTitleLabel.setText(album.getName());

        photosListView.setItems(FXCollections.observableArrayList(album.getPhotos()));
        photosListView.setCellFactory(lv -> new PhotoCell());
        photosListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> displayPhoto(sel));

        if (!album.getPhotos().isEmpty())
            photosListView.getSelectionModel().selectFirst();
    }

    // ── Navigation ────────────────────────────────────────────────────

    /**
     * Selects the previous photo in the list (slideshow backward).
     *
     * @param event button action event
     */
    @FXML
    private void onPrev(ActionEvent event) {
        int idx = photosListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) photosListView.getSelectionModel().select(idx - 1);
    }

    /**
     * Selects the next photo in the list (slideshow forward).
     *
     * @param event button action event
     */
    @FXML
    private void onNext(ActionEvent event) {
        int idx = photosListView.getSelectionModel().getSelectedIndex();
        if (idx < photosListView.getItems().size() - 1)
            photosListView.getSelectionModel().select(idx + 1);
    }

    // ── Photo management ─────────────────────────────────────────────

    /**
     * Opens a file chooser, imports the selected image into this album,
     * and reads its last-modified timestamp as the "date taken".
     * Reuses an existing {@link Photo} object if the file is already in another album.
     *
     * @param event button action event
     */
    @FXML
    private void onAddPhoto(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add Photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(stage());
        if (file == null) return;

        String absPath = file.getAbsolutePath();
        Album album = AppState.getCurrentAlbum();

        // Reject duplicates within the same album.
        boolean alreadyIn = album.getPhotos().stream()
                .anyMatch(p -> p.getAbsolutePath().equals(absPath));
        if (alreadyIn) { alert("This photo is already in the album."); return; }

        // Reuse the existing Photo object if it already lives in another album.
        Photo photo = findPhotoInUser(absPath);
        if (photo == null) {
            photo = new Photo(file.toPath().toAbsolutePath());
            try {
                photo.setDateTaken(Files.getLastModifiedTime(file.toPath()).toInstant());
            } catch (IOException ignored) {}
        }

        album.getPhotos().add(photo);
        photosListView.getItems().add(photo);
        photosListView.getSelectionModel().select(photo);
        saveUser();
    }

    /**
     * Removes the selected photo from this album.
     * The photo object is NOT deleted from other albums.
     *
     * @param event button action event
     */
    @FXML
    private void onRemovePhoto(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo to remove."); return; }

        AppState.getCurrentAlbum().getPhotos().remove(selected);
        photosListView.getItems().remove(selected);
        clearDisplay();
        saveUser();
    }

    /**
     * Prompts the user to set or update the caption of the selected photo.
     * Because a photo may appear in multiple albums, the new caption is visible everywhere.
     *
     * @param event button action event
     */
    @FXML
    private void onRecaption(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo first."); return; }

        TextInputDialog dlg = new TextInputDialog(selected.getCaption());
        dlg.setTitle("Caption");
        dlg.setHeaderText(null);
        dlg.setContentText("Caption:");
        dlg.showAndWait().ifPresent(cap -> {
            selected.setCaption(cap.trim());
            refreshCell(selected);
            displayPhoto(selected);
            saveUser();
        });
    }

    // ── Tag management ────────────────────────────────────────────────

    /**
     * Opens the Add Tag FXML dialog, validates the input, enforces single-value
     * constraints for tag types that do not allow multiple values, and adds the tag.
     *
     * @param event button action event
     */
    @FXML
    private void onAddTag(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo first."); return; }

        Map<String, Boolean> tagTypes = DataStore.loadTagTypes();

        try {
            FXMLLoader loader = new FXMLLoader(Photos.class.getResource("view/AddTag.fxml"));
            DialogPane pane = loader.load();
            AddTagController ctrl = loader.getController();
            ctrl.init(tagTypes);

            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Add Tag");
            dlg.setDialogPane(pane);
            Optional<ButtonType> result = dlg.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) return;

            String type  = ctrl.getTagType();
            String value = ctrl.getTagValue();

            if (type.isEmpty() || value.isEmpty()) {
                alert("Tag type and value cannot be empty.");
                return;
            }

            // Register a new tag type if the user typed one we don't know.
            if (!tagTypes.containsKey(type)) {
                tagTypes.put(type, ctrl.isMultipleAllowed());
                try { DataStore.saveTagTypes(tagTypes); } catch (IOException ignored) {}
            }

            boolean allowsMultiple = tagTypes.getOrDefault(type, true);

            // Enforce single-value constraint.
            if (!allowsMultiple) {
                boolean typeAlreadySet = selected.getTags().stream()
                        .anyMatch(t -> t.getName().equalsIgnoreCase(type));
                if (typeAlreadySet) {
                    alert("Tag type \"" + type + "\" only allows one value per photo.");
                    return;
                }
            }

            Tag tag = new Tag(type, value);
            if (selected.getTags().contains(tag)) {
                alert("This tag already exists on the photo.");
                return;
            }

            selected.getTags().add(tag);
            displayPhoto(selected);
            saveUser();

        } catch (IOException e) {
            alert("Could not open tag dialog: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to pick a tag to delete from the selected photo.
     *
     * @param event button action event
     */
    @FXML
    private void onDeleteTag(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo first."); return; }
        if (selected.getTags().isEmpty()) { alert("This photo has no tags."); return; }

        List<String> tagStrings = selected.getTags().stream()
                .map(Tag::toString).collect(Collectors.toList());
        ChoiceDialog<String> dlg = new ChoiceDialog<>(tagStrings.get(0), tagStrings);
        dlg.setTitle("Delete Tag");
        dlg.setHeaderText(null);
        dlg.setContentText("Select tag to delete:");
        dlg.showAndWait().ifPresent(tagStr -> {
            selected.getTags().removeIf(t -> t.toString().equals(tagStr));
            displayPhoto(selected);
            saveUser();
        });
    }

    // ── Copy / Move ───────────────────────────────────────────────────

    /**
     * Copies the selected photo to another album chosen by the user.
     *
     * @param event button action event
     */
    @FXML
    private void onCopyTo(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo first."); return; }
        Album target = pickTargetAlbum("Copy To");
        if (target == null) return;

        if (target.getPhotos().stream()
                .anyMatch(p -> p.getAbsolutePath().equals(selected.getAbsolutePath()))) {
            alert("That album already contains this photo.");
            return;
        }
        target.getPhotos().add(selected);
        saveUser();
    }

    /**
     * Moves the selected photo to another album and removes it from this album.
     *
     * @param event button action event
     */
    @FXML
    private void onMoveTo(ActionEvent event) {
        Photo selected = selected();
        if (selected == null) { alert("Select a photo first."); return; }
        Album target = pickTargetAlbum("Move To");
        if (target == null) return;

        if (target.getPhotos().stream()
                .anyMatch(p -> p.getAbsolutePath().equals(selected.getAbsolutePath()))) {
            alert("That album already contains this photo.");
            return;
        }
        target.getPhotos().add(selected);
        AppState.getCurrentAlbum().getPhotos().remove(selected);
        photosListView.getItems().remove(selected);
        clearDisplay();
        saveUser();
    }

    // ── Navigation back ───────────────────────────────────────────────

    /**
     * Saves and returns to the album list screen.
     *
     * @param event button action event
     */
    @FXML
    private void onBack(ActionEvent event) {
        saveUser();
        Stage stage = stage();
        try {
            AlbumsController ctrl = SceneRouter.go(stage, "view/Albums.fxml",
                    "Photos – " + AppState.getCurrentUser().getUsername());
            ctrl.init();
        } catch (Exception e) {
            alert("Navigation error: " + e.getMessage());
        }
    }

    /**
     * Saves and logs the user out, returning to the login screen.
     *
     * @param event button action event
     */
    @FXML
    private void onLogout(ActionEvent event) {
        saveUser();
        AppState.clear();
        try {
            SceneRouter.go(stage(), "view/Login.fxml", "Photos");
        } catch (Exception e) {
            stage().close();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────

    /** Updates the right-side detail pane for {@code photo}. */
    private void displayPhoto(Photo photo) {
        if (photo == null) { clearDisplay(); return; }
        try {
            photoImageView.setImage(new Image(fileUri(photo.getAbsolutePath()),
                    0, 0, true, true, true));
        } catch (Exception e) {
            photoImageView.setImage(null);
        }
        captionLabel.setText("Caption: " +
                (photo.getCaption().isEmpty() ? "(none)" : photo.getCaption()));
        dateLabel.setText("Date: " + (photo.getDateTaken() == null
                ? "(unknown)" : DATE_FMT.format(photo.getDateTaken())));
        String tags = photo.getTags().stream().map(Tag::toString)
                .collect(Collectors.joining(",  "));
        tagsLabel.setText("Tags: " + (tags.isEmpty() ? "(none)" : tags));
    }

    private void clearDisplay() {
        photoImageView.setImage(null);
        captionLabel.setText("Caption:");
        dateLabel.setText("Date:");
        tagsLabel.setText("Tags:");
    }

    /**
     * Finds an existing {@link Photo} object for {@code absPath} across all user albums,
     * so that edits (caption, tags) are shared wherever the photo appears.
     */
    private Photo findPhotoInUser(String absPath) {
        for (Album album : AppState.getCurrentUser().getAlbums()) {
            for (Photo p : album.getPhotos()) {
                if (p.getAbsolutePath().equals(absPath)) return p;
            }
        }
        return null;
    }

    /** Opens a ChoiceDialog for the user to pick a destination album (excluding current). */
    private Album pickTargetAlbum(String title) {
        List<Album> others = AppState.getCurrentUser().getAlbums().stream()
                .filter(a -> a != AppState.getCurrentAlbum())
                .collect(Collectors.toList());
        if (others.isEmpty()) { alert("No other albums exist."); return null; }
        List<String> names = others.stream().map(Album::getName).collect(Collectors.toList());
        ChoiceDialog<String> dlg = new ChoiceDialog<>(names.get(0), names);
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.setContentText("Destination album:");
        Optional<String> chosen = dlg.showAndWait();
        if (chosen.isEmpty()) return null;
        return others.stream().filter(a -> a.getName().equals(chosen.get())).findFirst().orElse(null);
    }

    /** Forces the ListView to redraw the cell for {@code photo}. */
    private void refreshCell(Photo photo) {
        int idx = photosListView.getItems().indexOf(photo);
        if (idx >= 0) {
            photosListView.getItems().set(idx, photo);
            photosListView.getSelectionModel().select(photo);
        }
    }

    private Photo selected() {
        return photosListView.getSelectionModel().getSelectedItem();
    }

    private Stage stage() {
        return (Stage) photosListView.getScene().getWindow();
    }

    private void saveUser() {
        try {
            DataStore.saveUser(AppState.getCurrentUser());
        } catch (IOException e) {
            alert("Save error: " + e.getMessage());
        }
    }

    private static void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private static String fileUri(String absolutePath) {
        return Path.of(absolutePath).toUri().toString();
    }

    // ── Inner cell class ──────────────────────────────────────────────

    /**
     * Custom ListCell that shows a 60×60 thumbnail on the left
     * and the caption on the right for each photo.
     */
    private static class PhotoCell extends ListCell<Photo> {

        private final ImageView thumb   = new ImageView();
        private final Label     caption = new Label();
        private final javafx.scene.layout.HBox box =
                new javafx.scene.layout.HBox(10, thumb, caption);

        PhotoCell() {
            thumb.setFitWidth(60);
            thumb.setFitHeight(60);
            thumb.setPreserveRatio(true);
            caption.setWrapText(true);
        }

        @Override
        protected void updateItem(Photo photo, boolean empty) {
            super.updateItem(photo, empty);
            if (empty || photo == null) {
                setGraphic(null);
                return;
            }
            try {
                thumb.setImage(new Image(fileUri(photo.getAbsolutePath()),
                        60, 60, true, false, false));
            } catch (Exception e) {
                thumb.setImage(null);
            }
            caption.setText(photo.getCaption().isEmpty() ? "(no caption)" : photo.getCaption());
            setGraphic(box);
        }
    }
}
