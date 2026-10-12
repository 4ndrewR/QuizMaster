package com.quizmaster.views;

import com.quizmaster.models.Student;
import com.quizmaster.services.AuthService;
import com.quizmaster.services.DataStore;
import com.quizmaster.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Registration screen. Self-service sign-up creates STUDENT accounts only;
 * teachers are seeded by DataStore or promoted by an existing teacher.
 */
public class RegisterView implements SceneManager.View {

    private final BorderPane root;

    public RegisterView() {
        root = new BorderPane();
        root.getStyleClass().add("auth-root");
        root.setCenter(buildCard());
    }

    private VBox buildCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36));

        Text title = new Text("Create Student Account");
        title.getStyleClass().add("title");

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First name");
        firstNameField.getStyleClass().add("text-input");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last name");
        lastNameField.getStyleClass().add("text-input");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-input");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 chars)");
        passwordField.getStyleClass().add("text-input");

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Date of birth");
        dobPicker.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().add("primary-btn");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = new Button("Back to login");
        backBtn.getStyleClass().add("link-btn");

        registerBtn.setOnAction(e -> handleRegister(firstNameField.getText(),
                lastNameField.getText(), usernameField.getText(),
                passwordField.getText(),
                dobPicker.getValue() == null ? "" : dobPicker.getValue().toString(),
                errorLabel));
        backBtn.setOnAction(e -> SceneManager.switchTo(new LoginView()));

        HBox names = new HBox(10, firstNameField, lastNameField);
        HBox.setHgrow(firstNameField, Priority.ALWAYS);
        HBox.setHgrow(lastNameField, Priority.ALWAYS);

        card.getChildren().addAll(title,
                names,
                new Label("Username"), usernameField,
                new Label("Password"), passwordField,
                new Label("Date of birth"), dobPicker,
                errorLabel, registerBtn, backBtn);
        return card;
    }

    private void handleRegister(String first, String last, String username,
                                String password, String dob, Label errorLabel) {
        if (first.isBlank() || last.isBlank() || username.isBlank() || password.isBlank()) {
            errorLabel.setText("All fields except DOB are required.");
            errorLabel.setVisible(true);
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            errorLabel.setVisible(true);
            return;
        }
        Student newStudent = new Student(username.trim(), AuthService.hash(password), first.trim(), last.trim(), dob);
        boolean added = DataStore.getInstance().addUser(newStudent);
        if (!added) {
            errorLabel.setText("Username already exists. Choose another.");
            errorLabel.setVisible(true);
            return;
        }
        SceneManager.showAlert(Alert.AlertType.INFORMATION, "Account created",
                "Welcome, " + first + "! You can now log in.\n\n"
                        + "Tip: open \"My Subjects\" to enroll before taking a quiz.");
        SceneManager.switchTo(new LoginView());
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
