package com.quizmaster.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Centralizes scene switching so views don't have to know about the Stage.
 *
 * Each view exposes a getRoot() method returning a JavaFX Parent. SceneManager
 * wraps that root in a Scene with the shared stylesheet applied.
 */
public class SceneManager {

    private static Stage primaryStage;
    private static final String STYLESHEET = "/styles.css";

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /** A view supplies its root node through this interface. */
    public interface View {
        Parent getRoot();
    }

    /** Switch the stage to the given view, applying the shared stylesheet. */
    public static void switchTo(View view) {
        Scene scene = new Scene(view.getRoot(), 1000, 680);
        var cssUrl = SceneManager.class.getResource(STYLESHEET);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setScene(scene);
    }

    public static Stage getStage() {
        return primaryStage;
    }

    /** Convenience: show a simple alert dialog. */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
