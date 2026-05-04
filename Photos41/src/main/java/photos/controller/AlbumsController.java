package photos.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import photos.model.Album;
import photos.model.AppState;
import photos.model.DataStore;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller for the user's album list screen.
 * Displays every album with its name, photo count, and date range.
 * Supports create, rename, delete, and open operations on albums.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class AlbumsController {

    @FXML private Label           usernameLabel;
    @FXML private ListView<Album> albumsListView;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    /** Creates the controller for the albums overview scene. */
    public AlbumsController() {}

    /**
     * Populates the album list from the current user in {@link AppState}.
     * Called by the previous controller after the scene is loaded.
     */
    public void init() {
        usernameLabel.setText(AppState.getCurrentUser().getUsername());
        refreshList();
        albumsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Album album, boolean empty) {
                super.updateItem(album, empty);
                setText(empty || album == null ? null : formatAlbum(album));
            }
        });
    }

    // ── Album operations ─────────────────────────────────────────────

    /**
     * Prompts for a new album name and creates an empty album.
     *
     * @param event button action event
     */
    @FXML
    private void onCreateAlbum(ActionEvent event) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Create Album");
        dlg.setHeaderText(null);
        dlg.setContentText("Album name:");
        Optional<String> result = dlg.showAndWait();
        result.ifPresent(name -> {
            name = name.trim();
            if (name.isEmpty()) { alert("Album name cannot be empty."); return; }
            if (albumExists(name)) { alert("An album named \"" + name + "\" already exists."); return; }
            Album album = new Album(name);
            AppState.getCurrentUser().getAlbums().add(album);
            saveUser();
            refreshList();
        });
    }

    /**
     * Prompts for a new name and renames the selected album.
     *
     * @param event button action event
     */
    @FXML
    private void onRenameAlbum(ActionEvent event) {
        Album selected = albumsListView.getSelectionModel().getSelectedItem();
        if (selected == null) { alert("Select an album to rename."); return; }

        TextInputDialog dlg = new TextInputDialog(selected.getName());
        dlg.setTitle("Rename Album");
        dlg.setHeaderText(null);
        dlg.setContentText("New name:");
        Optional<String> result = dlg.showAndWait();
        result.ifPresent(name -> {
            name = name.trim();
            if (name.isEmpty()) { alert("Album name cannot be empty."); return; }
            if (!name.equals(selected.getName()) && albumExists(name)) {
                alert("An album named \"" + name + "\" already exists.");
                return;
            }
            selected.setName(name);
            saveUser();
            refreshList();
        });
    }

    /**
     * Deletes the selected album (does not delete photos from other albums).
     *
     * @param event button action event
     */
    @FXML
    private void onDeleteAlbum(ActionEvent event) {
        Album selected = albumsListView.getSelectionModel().getSelectedItem();
        if (selected == null) { alert("Select an album to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete album \"" + selected.getName() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                AppState.getCurrentUser().getAlbums().remove(selected);
                saveUser();
                refreshList();
            }
        });
    }

    /**
     * Opens the selected album in the album detail view.
     *
     * @param event button action event
     */
    @FXML
    private void onOpenAlbum(ActionEvent event) {
        Album selected = albumsListView.getSelectionModel().getSelectedItem();
        if (selected == null) { alert("Select an album to open."); return; }

        AppState.setCurrentAlbum(selected);
        Stage stage = (Stage) albumsListView.getScene().getWindow();
        try {
            AlbumController ctrl = SceneRouter.go(stage, "view/Album.fxml",
                    "Photos – " + selected.getName());
            ctrl.init();
        } catch (Exception e) {
            alert("Could not open album: " + e.getMessage());
        }
    }

    /**
     * Navigates to the search screen.
     *
     * @param event button action event
     */
    @FXML
    private void onSearch(ActionEvent event) {
        Stage stage = (Stage) albumsListView.getScene().getWindow();
        try {
            SearchController ctrl = SceneRouter.go(stage, "view/Search.fxml", "Photos – Search");
            ctrl.init();
        } catch (Exception e) {
            alert("Could not open search: " + e.getMessage());
        }
    }

    /**
     * Saves the current user's data and returns to the login screen.
     *
     * @param event button action event
     */
    @FXML
    private void onLogout(ActionEvent event) {
        saveUser();
        AppState.clear();
        Stage stage = (Stage) albumsListView.getScene().getWindow();
        try {
            SceneRouter.go(stage, "view/Login.fxml", "Photos");
        } catch (Exception e) {
            stage.close();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────

    private void refreshList() {
        int sel = albumsListView.getSelectionModel().getSelectedIndex();
        albumsListView.setItems(FXCollections.observableArrayList(
                AppState.getCurrentUser().getAlbums()));
        if (sel >= 0 && sel < albumsListView.getItems().size())
            albumsListView.getSelectionModel().select(sel);
    }

    private boolean albumExists(String name) {
        return AppState.getCurrentUser().getAlbums().stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(name));
    }

    /** Formats an album's display string: name • photo count • date range. */
    private String formatAlbum(Album a) {
        int count = a.getPhotos().size();
        Instant earliest = a.getEarliestDate();
        Instant latest   = a.getLatestDate();
        String dates = (earliest == null)
                ? "no photos"
                : DATE_FMT.format(earliest) + " – " + DATE_FMT.format(latest);
        return String.format("%s  •  %d photo%s  •  %s",
                a.getName(), count, count == 1 ? "" : "s", dates);
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
}
