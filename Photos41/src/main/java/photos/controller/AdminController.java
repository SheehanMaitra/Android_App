package photos.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import photos.model.AppState;
import photos.model.DataStore;
import photos.model.User;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the admin subsystem.
 * Allows the admin to list all registered users, create new ones, and delete existing ones.
 * Admin cannot delete the built-in "stock" user.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class AdminController {

    @FXML private ListView<String> usersListView;
    @FXML private TextField        newUsernameField;

    /** Creates the controller for the admin subsystem scene. */
    public AdminController() {}

    /**
     * Populates the user list from disk. Called by {@link LoginController} after the
     * scene is loaded.
     */
    public void init() {
        List<String> names = DataStore.loadUsernames();
        usersListView.setItems(FXCollections.observableArrayList(names));
    }

    /**
     * Creates a new user with the name entered in the text field.
     * Shows an error alert if the name is blank, reserved, or already taken.
     *
     * @param event button action event
     */
    @FXML
    private void onCreateUser(ActionEvent event) {
        String name = newUsernameField.getText() == null
                ? "" : newUsernameField.getText().trim();

        if (name.isEmpty()) {
            alert("Please enter a username.");
            return;
        }
        if (name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("stock")) {
            alert("\"" + name + "\" is a reserved username.");
            return;
        }
        boolean duplicate = usersListView.getItems().stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(name));
        if (duplicate) {
            alert("User \"" + name + "\" already exists.");
            return;
        }

        try {
            List<String> allNames = DataStore.loadUsernames();
            allNames.add(name);
            DataStore.saveUsernames(allNames);
            DataStore.saveUser(new User(name));
            usersListView.getItems().add(name);
            newUsernameField.clear();
        } catch (IOException e) {
            alert("Could not save user: " + e.getMessage());
        }
    }

    /**
     * Deletes the user currently selected in the list.
     * Refuses to delete the built-in "stock" user.
     *
     * @param event button action event
     */
    @FXML
    private void onDeleteSelected(ActionEvent event) {
        String selected = usersListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Select a user to delete.");
            return;
        }
        if (selected.equalsIgnoreCase("stock")) {
            alert("The \"stock\" user cannot be deleted.");
            return;
        }

        try {
            List<String> allNames = DataStore.loadUsernames();
            allNames.remove(selected);
            DataStore.saveUsernames(allNames);
            DataStore.deleteUserFile(selected);
            usersListView.getItems().remove(selected);
        } catch (IOException e) {
            alert("Could not delete user: " + e.getMessage());
        }
    }

    /**
     * Logs out the admin and returns to the login screen.
     *
     * @param event button action event
     */
    @FXML
    private void onLogout(ActionEvent event) {
        AppState.clear();
        Stage stage = (Stage) usersListView.getScene().getWindow();
        try {
            SceneRouter.go(stage, "view/Login.fxml", "Photos");
        } catch (Exception e) {
            stage.close();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
