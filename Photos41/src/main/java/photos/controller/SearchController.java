package photos.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import photos.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the search screen.
 * Supports two mutually exclusive search modes:
 * <ol>
 *   <li>Date range – photos whose date falls within [startDate, endDate].</li>
 *   <li>Tag – single tag, or two tags combined with AND or OR.</li>
 * </ol>
 * Results are shown in a thumbnail list; a "Create Album" button saves them.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class SearchController {

    // ── Search-mode controls ──────────────────────────────────────────
    @FXML private ToggleGroup  searchModeGroup;
    @FXML private RadioButton  dateModeRadio;
    @FXML private RadioButton  tagModeRadio;

    // ── Date-range controls ───────────────────────────────────────────
    @FXML private DatePicker   startDatePicker;
    @FXML private DatePicker   endDatePicker;

    // ── Tag controls ──────────────────────────────────────────────────
    @FXML private ComboBox<String> tag1TypeCombo;
    @FXML private TextField        tag1ValueField;
    @FXML private ComboBox<String> tag2TypeCombo;
    @FXML private TextField        tag2ValueField;
    @FXML private RadioButton      andRadio;
    @FXML private RadioButton      orRadio;

    // ── Result area ───────────────────────────────────────────────────
    @FXML private ListView<Photo>  resultsListView;
    @FXML private ImageView        resultImageView;
    @FXML private Label            resultCaptionLabel;
    @FXML private Label            resultDateLabel;
    @FXML private Label            resultTagsLabel;

    private List<Photo> currentResults = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a").withZone(ZoneId.systemDefault());

    /** Creates the controller for the search scene. */
    public SearchController() {}

    // ── Lifecycle ─────────────────────────────────────────────────────

    /**
     * Populates tag-type combos from the saved registry.
     * Called by JavaFX after FXML injection and by {@link AlbumsController} after scene load.
     */
    public void init() {
        Map<String, Boolean> tagTypes = DataStore.loadTagTypes();
        tag1TypeCombo.getItems().setAll(tagTypes.keySet());
        tag2TypeCombo.getItems().setAll(tagTypes.keySet());

        resultsListView.setCellFactory(lv -> new PhotoCell());
        resultsListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> displayResult(sel));
    }

    /** Called by JavaFX after FXML nodes are injected. Sets up the operator radio group. */
    @FXML
    private void initialize() {
        ToggleGroup opGroup = new ToggleGroup();
        andRadio.setToggleGroup(opGroup);
        orRadio.setToggleGroup(opGroup);
        andRadio.setSelected(true);
    }

    // ── Search ────────────────────────────────────────────────────────

    /**
     * Runs the search based on the selected mode and displays results.
     *
     * @param event button action event
     */
    @FXML
    private void onSearch(ActionEvent event) {
        List<Photo> allPhotos = getAllUserPhotos();

        if (dateModeRadio.isSelected()) {
            currentResults = searchByDate(allPhotos);
        } else {
            currentResults = searchByTags(allPhotos);
        }

        if (currentResults == null) return; // validation failed
        resultsListView.setItems(FXCollections.observableArrayList(currentResults));
        clearDetail();
    }

    /**
     * Prompts for an album name and creates a new album containing the search results.
     *
     * @param event button action event
     */
    @FXML
    private void onCreateAlbumFromResults(ActionEvent event) {
        if (currentResults.isEmpty()) {
            alert("No search results to save.");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Create Album");
        dlg.setHeaderText(null);
        dlg.setContentText("New album name:");
        dlg.showAndWait().ifPresent(rawName -> {
            final String name = rawName.trim();
            if (name.isEmpty()) { alert("Album name cannot be empty."); return; }
            boolean dup = AppState.getCurrentUser().getAlbums().stream()
                    .anyMatch(a -> a.getName().equalsIgnoreCase(name));
            if (dup) { alert("An album named \"" + name + "\" already exists."); return; }

            Album album = new Album(name);
            album.getPhotos().addAll(currentResults);
            AppState.getCurrentUser().getAlbums().add(album);
            try {
                DataStore.saveUser(AppState.getCurrentUser());
                alert(Alert.AlertType.INFORMATION,
                        "Album \"" + name + "\" created with " + currentResults.size() + " photo(s).");
            } catch (IOException e) {
                alert("Save error: " + e.getMessage());
            }
        });
    }

    /**
     * Returns to the album list screen.
     *
     * @param event button action event
     */
    @FXML
    private void onBack(ActionEvent event) {
        Stage stage = (Stage) resultsListView.getScene().getWindow();
        try {
            AlbumsController ctrl = SceneRouter.go(stage, "view/Albums.fxml",
                    "Photos – " + AppState.getCurrentUser().getUsername());
            ctrl.init();
        } catch (Exception e) {
            alert("Navigation error: " + e.getMessage());
        }
    }

    // ── Search logic ──────────────────────────────────────────────────

    /**
     * Filters photos by date range. Requires both pickers to have a value;
     * the end date is inclusive.
     *
     * @param photos pool of photos to search
     * @return matching photos, or {@code null} if input is invalid
     */
    private List<Photo> searchByDate(List<Photo> photos) {
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null || end == null) {
            alert("Please select both a start and end date.");
            return null;
        }
        if (end.isBefore(start)) {
            alert("End date must be on or after start date.");
            return null;
        }
        var startInst = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        var endInst   = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return photos.stream()
                .filter(p -> p.getDateTaken() != null
                        && !p.getDateTaken().isBefore(startInst)
                        && p.getDateTaken().isBefore(endInst))
                .collect(Collectors.toList());
    }

    /**
     * Filters photos by tag criteria. Supports a single tag,
     * or two tags combined with AND / OR.
     *
     * @param photos pool of photos to search
     * @return matching photos, or {@code null} if input is invalid
     */
    private List<Photo> searchByTags(List<Photo> photos) {
        String t1Type  = tag1TypeCombo.getEditor().getText();
        String t1Value = tag1ValueField.getText();
        String t2Type  = tag2TypeCombo.getEditor().getText();
        String t2Value = tag2ValueField.getText();

        t1Type  = t1Type  == null ? "" : t1Type.trim();
        t1Value = t1Value == null ? "" : t1Value.trim();
        t2Type  = t2Type  == null ? "" : t2Type.trim();
        t2Value = t2Value == null ? "" : t2Value.trim();

        if (t1Type.isEmpty() || t1Value.isEmpty()) {
            alert("Tag 1 type and value are required.");
            return null;
        }

        boolean useSecond = !t2Type.isEmpty() || !t2Value.isEmpty();
        if (useSecond && (t2Type.isEmpty() || t2Value.isEmpty())) {
            alert("If using a second tag, both type and value are required.");
            return null;
        }

        final String ft1 = t1Type, fv1 = t1Value, ft2 = t2Type, fv2 = t2Value;

        return photos.stream().filter(p -> {
            boolean m1 = hasTag(p, ft1, fv1);
            if (!useSecond) return m1;
            boolean m2 = hasTag(p, ft2, fv2);
            return andRadio.isSelected() ? (m1 && m2) : (m1 || m2);
        }).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Returns a de-duplicated list of all photos across all user albums. */
    private List<Photo> getAllUserPhotos() {
        return AppState.getCurrentUser().getAlbums().stream()
                .flatMap(a -> a.getPhotos().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /** Checks whether {@code photo} has a tag matching the given type and value (case-insensitive). */
    private static boolean hasTag(Photo photo, String type, String value) {
        return photo.getTags().stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(type)
                            && t.getValue().equalsIgnoreCase(value));
    }

    /** Updates the right-side detail panel for the selected result photo. */
    private void displayResult(Photo photo) {
        if (photo == null) { clearDetail(); return; }
        try {
            resultImageView.setImage(new Image(fileUri(photo.getAbsolutePath()),
                    0, 0, true, true, true));
        } catch (Exception e) {
            resultImageView.setImage(null);
        }
        resultCaptionLabel.setText("Caption: " +
                (photo.getCaption().isEmpty() ? "(none)" : photo.getCaption()));
        resultDateLabel.setText("Date: " + (photo.getDateTaken() == null
                ? "(unknown)" : DATE_FMT.format(photo.getDateTaken())));
        String tags = photo.getTags().stream().map(Tag::toString).collect(Collectors.joining(",  "));
        resultTagsLabel.setText("Tags: " + (tags.isEmpty() ? "(none)" : tags));
    }

    private void clearDetail() {
        resultImageView.setImage(null);
        resultCaptionLabel.setText("Caption:");
        resultDateLabel.setText("Date:");
        resultTagsLabel.setText("Tags:");
    }

    private static void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private static void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }

    private static String fileUri(String absolutePath) {
        return Path.of(absolutePath).toUri().toString();
    }

    // ── Inner cell ────────────────────────────────────────────────────

    /** Thumbnail cell matching the style used in {@link AlbumController}. */
    private static class PhotoCell extends ListCell<Photo> {
        private final ImageView thumb   = new ImageView();
        private final Label     caption = new Label();
        private final javafx.scene.layout.HBox box =
                new javafx.scene.layout.HBox(10, thumb, caption);

        PhotoCell() {
            thumb.setFitWidth(60);
            thumb.setFitHeight(60);
            thumb.setPreserveRatio(true);
        }

        @Override
        protected void updateItem(Photo photo, boolean empty) {
            super.updateItem(photo, empty);
            if (empty || photo == null) { setGraphic(null); return; }
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
