package com.quizmaster;

import com.quizmaster.services.DataStore;
import com.quizmaster.utils.SceneManager;
import com.quizmaster.views.LoginView;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * QuizMaster - JavaFX Trivia Quiz Application
 *
 * Application entry point. Bootstraps the data store (which seeds the default
 * admin user and sample questions on first run), then shows the login screen.
 *
 * Default admin credentials (per project spec):
 *   username: ProAnds
 *   password: JavaProject123
 */
public class Main extends Application {

    public static final String APP_TITLE = "QuizMaster";

    @Override
    public void start(Stage primaryStage) {
        // Initialize data storage and seed default data on first launch.
        DataStore.getInstance().initialize();

        // Configure the primary stage.
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(640);

        // Hand the stage to the SceneManager so any view can switch scenes.
        SceneManager.init(primaryStage);

        // Show the login screen first.
        SceneManager.switchTo(new LoginView());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
