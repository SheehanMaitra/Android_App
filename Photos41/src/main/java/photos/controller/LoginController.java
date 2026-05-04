package photos.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import photos.model.AppState;
import photos.model.DataStore;
import photos.model.User;

import java.util.List;

/**
 * Controller for the login screen.
 * Authenticates against the registered-username list and routes to the
 * admin subsystem or the user album view accordingly.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Label     statusLabel;

    /** Creates the controller for the login scene. */
    public LoginController() {}

    /**
     * Validates the entered username and navigates to the appropriate scene.
     * "admin" goes to the admin panel; any other registered username loads
     * that user's data and goes to the albums view.
     *
     * @param event button action event
     */
    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField.getText() == null
                ? "" : usernameField.getText().trim();

        if (username.isEmpty()) {
            statusLabel.setText("Please enter a username.");
            return;
        }

        Stage stage = (Stage) usernameField.getScene().getWindow();
        try {
            if ("admin".equalsIgnoreCase(username)) {
                AdminController ctrl = SceneRouter.go(stage, "view/Admin.fxml", "Photos – Admin");
                ctrl.init();
            } else {
                List<String> registered = DataStore.loadUsernames();
                String matchedUsername = registered.stream()
                        .filter(name -> name.equalsIgnoreCase(username))
                        .findFirst()
                        .orElse(null);
                if (matchedUsername == null) {
                    statusLabel.setText("User \"" + username + "\" not found. Ask admin to create an account.");
                    return;
                }
                User user = DataStore.loadUser(matchedUsername);
                AppState.setCurrentUser(user);
                AlbumsController ctrl = SceneRouter.go(stage, "view/Albums.fxml",
                        "Photos – " + matchedUsername);
                ctrl.init();
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Closes the application when the Quit button is pressed.
     *
     * @param event button action event
     */
    @FXML
    private void onQuit(ActionEvent event) {
        ((Stage) usernameField.getScene().getWindow()).close();
    }
}
