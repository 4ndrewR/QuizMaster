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
 * Teacher dashboard. Tile-based home giving access to every teacher feature:
 * manage users (incl. promote/demote), manage questions, subject overview,
 * reports, leaderboard, and a personal PDF export.
 */
public class TeacherDashboardView implements SceneManager.View {

    private final BorderPane root;

    public TeacherDashboardView() {
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

        Label title = new Label("Teacher Control Panel");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(AuthService.getCurrentUser().getFullName() + "  \u2022  TEACHER");
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

        grid.add(Tiles.tile("Manage Users", "View, delete, and promote/demote users",
                e -> SceneManager.switchTo(new ManageUsersView())), 0, 0);
        grid.add(Tiles.tile("Manage Questions", "Add new questions or remove old ones",
                e -> SceneManager.switchTo(new ManageQuestionsView())), 1, 0);
        grid.add(Tiles.tile("Subjects", "Question counts and enrolled students",
                e -> SceneManager.switchTo(new TeacherSubjectsView())), 2, 0);

        grid.add(Tiles.tile("View Reports", "Grades, pass rates, and game history",
                e -> SceneManager.switchTo(new ViewReportsView())), 0, 1);
        grid.add(Tiles.tile("Leaderboard", "Top scoring students",
                e -> SceneManager.switchTo(new LeaderboardView())), 1, 1);
        grid.add(Tiles.tile("My Profile PDF", "Export your teacher info as PDF",
                e -> exportPdf()), 2, 1);

        return grid;
    }

    private void exportPdf() {
        try {
            File file = PdfService.generateUserInfoPdf(AuthService.getCurrentUser());
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "PDF Generated",
                    "Saved to:\n" + file.getAbsolutePath());
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
