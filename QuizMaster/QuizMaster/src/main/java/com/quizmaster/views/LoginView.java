package com.quizmaster.views;

import com.quizmaster.models.Teacher;
import com.quizmaster.models.User;
import com.quizmaster.services.AuthService;
import com.quizmaster.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.Optional;

/**
 * Login screen. The first view shown when the app launches.
 * Demonstrates EVENT HANDLING via lambda expressions, and POLYMORPHISM by
 * routing to a different dashboard based on the user's runtime subclass.
 */
public class LoginView implements SceneManager.View {

    private final BorderPane root;

    public LoginView() {
        root = new BorderPane();
        root.getStyleClass().add("auth-root");
        root.setCenter(buildCard());
    }

    private VBox buildCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));

        Text title = new Text("QuizMaster");
        title.getStyleClass().add("title");

        Text subtitle = new Text("Sign in to continue");
        subtitle.getStyleClass().add("subtitle");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-input");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-input");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        Button loginBtn = new Button("Log In");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button registerBtn = new Button("Create new student account");
        registerBtn.getStyleClass().add("link-btn");

        loginBtn.setOnAction(e -> handleLogin(usernameField.getText(),
                passwordField.getText(), errorLabel));
        passwordField.setOnAction(e -> loginBtn.fire());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        registerBtn.setOnAction(e -> SceneManager.switchTo(new RegisterView()));

        Label hint = new Label("First time? Try teacher: ProAnds / JavaProject123");
        hint.getStyleClass().add("hint-label");

        card.getChildren().addAll(title, subtitle,
                new Label("Username"), usernameField,
                new Label("Password"), passwordField,
                errorLabel, loginBtn, registerBtn, hint);
        return card;
    }

    private void handleLogin(String username, String password, Label errorLabel) {
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter both username and password.");
            errorLabel.setVisible(true);
            return;
        }
        Optional<User> user = AuthService.login(username.trim(), password);
        if (user.isEmpty()) {
            errorLabel.setText("Invalid username or password.");
            errorLabel.setVisible(true);
            return;
        }
        // POLYMORPHISM: route based on the actual subclass type.
        if (user.get() instanceof Teacher) {
            SceneManager.switchTo(new TeacherDashboardView());
        } else {
            SceneManager.switchTo(new StudentDashboardView());
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
