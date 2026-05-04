/**
 * JavaFX module descriptor for the Photos application.
 */
module photos {
    requires javafx.controls;
    requires javafx.fxml;

    opens photos.controller to javafx.fxml;
    opens photos.model;          // required for Java serialization (reflective field access)
    exports photos;
    exports photos.model;
}

