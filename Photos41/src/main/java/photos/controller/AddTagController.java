package photos.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Map;

/**
 * Controller for the Add Tag dialog ({@code AddTag.fxml}).
 * Exposes the entered tag type, value, and whether the new type
 * should allow multiple values per photo.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class AddTagController {

    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        valueField;
    @FXML private CheckBox         multipleCheckBox;

    /** Creates the controller used by the Add Tag FXML dialog. */
    public AddTagController() {}

    /**
     * Populates the tag-type combo with all currently registered types.
     * The "Allow multiple values" checkbox is hidden for existing types.
     *
     * @param tagTypes map of tag type name → allowsMultiple loaded from {@link photos.model.DataStore}
     */
    public void init(Map<String, Boolean> tagTypes) {
        typeCombo.getItems().setAll(tagTypes.keySet());

        // Show the checkbox only when the user types a brand-new type name.
        multipleCheckBox.setVisible(false);
        typeCombo.getEditor().textProperty().addListener((obs, old, text) -> {
            boolean isNew = text != null && !text.isBlank() && !tagTypes.containsKey(text.trim());
            multipleCheckBox.setVisible(isNew);
        });
        typeCombo.valueProperty().addListener((obs, old, val) -> {
            boolean isNew = val != null && !tagTypes.containsKey(val);
            multipleCheckBox.setVisible(isNew);
        });
    }

    /**
     * Returns the tag type entered or selected.
     *
     * @return trimmed tag type string, never {@code null}
     */
    public String getTagType() {
        String val = typeCombo.getEditor().getText();
        return val == null ? "" : val.trim();
    }

    /**
     * Returns the tag value entered by the user.
     *
     * @return trimmed tag value string, never {@code null}
     */
    public String getTagValue() {
        String val = valueField.getText();
        return val == null ? "" : val.trim();
    }

    /**
     * Returns {@code true} if the user checked "Allow multiple values" for a new tag type.
     *
     * @return whether the new tag type allows multiple values per photo
     */
    public boolean isMultipleAllowed() {
        return multipleCheckBox.isSelected();
    }
}
