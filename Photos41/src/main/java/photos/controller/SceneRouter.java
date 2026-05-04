package photos.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import photos.Photos;

import java.io.IOException;

/**
 * Utility for switching scenes on a {@link Stage}.
 * The {@link #go} method loads an FXML resource, swaps the scene,
 * and returns the new controller so callers can pass data to it.
 *
 * @author Sheehan Maitra
 * @author Anish Jha
 */
public final class SceneRouter {

    private SceneRouter() {}

    /**
     * Loads the given FXML resource, replaces the stage's current scene,
     * and returns the controller instance created by the FXML loader.
     *
     * @param <C>          controller type
     * @param stage        stage whose scene will be replaced
     * @param fxmlResource path to the FXML file relative to the {@code photos} package root
     * @param title        window title to set
     * @return the controller created by {@link FXMLLoader}
     * @throws IOException if the FXML resource cannot be loaded
     */
    public static <C> C go(Stage stage, String fxmlResource, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Photos.class.getResource(fxmlResource));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.sizeToScene();
        return loader.getController();
    }
}
