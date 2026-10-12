package com.quizmaster.views;

import com.quizmaster.services.AuthService;
import com.quizmaster.services.PdfService;
import com.quizmaster.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.io.File;

/**
 * Student dashboard. Tile-based home for student users: play quizzes, manage
 * subject enrollment, review scores/grades, see the leaderboard, export a PDF.
 */
public class StudentDashboardView implements SceneManager.View {

    private final BorderPane root;

    public StudentDashboardView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildTiles());
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Student Hub");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(AuthService.getCurrentUser().getFullName() + "  \u2022  STUDENT");
        userLabel.getStyleClass().add("header-user");

        Button logout = new Button("Logout");
        logout.getStyleClass().add("logout-btn");
        logout.setOnAction(e -> {
            AuthService.logout();
            SceneManager.switchTo(new LoginView());
        });

        header.getChildren().addAll(title, spacer, userLabel, logout);
        return header;
    }

    private GridPane buildTiles() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(40));
        grid.setAlignment(Pos.CENTER);

        grid.add(Tiles.tile("Play Quiz", "Take a graded quiz on an enrolled subject",
                e -> SceneManager.switchTo(new PlayQuizView())), 0, 0);
        grid.add(Tiles.tile("My Subjects", "Enroll in or drop subjects",
                e -> SceneManager.switchTo(new SubjectsView())), 1, 0);
        grid.add(Tiles.tile("My Scores", "Your grades and full quiz history",
                e -> SceneManager.switchTo(new MyScoresView())), 2, 0);

        grid.add(Tiles.tile("Leaderboard", "See the top students",
                e -> SceneManager.switchTo(new LeaderboardView())), 0, 1);
        grid.add(Tiles.tile("Download Report", "Save your profile + grades as PDF",
                e -> exportPdf()), 1, 1);

        return grid;
    }

    private void exportPdf() {
        try {
            File file = PdfService.generateUserInfoPdf(AuthService.getCurrentUser());
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "PDF Generated",
                    "Your report was saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            SceneManager.showAlert(Alert.AlertType.ERROR, "PDF Error",
                    "Could not generate PDF: " + ex.getMessage());
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
