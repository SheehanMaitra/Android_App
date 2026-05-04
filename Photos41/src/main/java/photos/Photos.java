package photos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import photos.model.AppState;
import photos.model.DataStore;

import java.io.IOException;

/**
 * Main JavaFX entry point for the Photos application.
 * Initialises persistent storage and shows the login screen on startup.
 * Saves the active user's data when the primary window is closed.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public class Photos extends Application {

    /** Creates the JavaFX application instance. */
    public Photos() {}

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Builds the initial scene (login) and registers a close handler that
     * saves the current user's data before the application exits.
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime
     * @throws IOException if the login FXML cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Seed stock user from data/stock/stock/ image files if not yet done.
        DataStore.initStockUserIfNeeded();

        FXMLLoader loader = new FXMLLoader(Photos.class.getResource("view/Login.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Photos");
        primaryStage.setScene(scene);

        // Save current user data when the user closes the window directly (quit).
        primaryStage.setOnCloseRequest(e -> saveCurrentUser());

        primaryStage.show();
    }

    /**
     * Called by the JavaFX runtime when the application is about to stop.
     * Ensures user data is saved even if {@code setOnCloseRequest} is not triggered.
     */
    @Override
    public void stop() {
        saveCurrentUser();
    }

    /** Persists the logged-in user's data, silently ignoring any I/O errors. */
    private static void saveCurrentUser() {
        if (AppState.getCurrentUser() != null) {
            try {
                DataStore.saveUser(AppState.getCurrentUser());
            } catch (IOException ignored) {}
        }
    }
}
