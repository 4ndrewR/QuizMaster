package com.quizmaster.views;

import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Teacher;
import com.quizmaster.services.AuthService;
import com.quizmaster.services.DataStore;
import com.quizmaster.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Top 20 scores across all students. Open to both teachers and students.
 * The back button routes to the dashboard matching the user's runtime subclass
 * (polymorphism). Ranking is by points; the grade is shown alongside.
 */
public class LeaderboardView implements SceneManager.View {

    private final BorderPane root;

    public LeaderboardView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildTable());
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 30, 16, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("logout-btn");
        back.setOnAction(e -> {
            if (AuthService.getCurrentUser() instanceof Teacher) {
                SceneManager.switchTo(new TeacherDashboardView());
            } else {
                SceneManager.switchTo(new StudentDashboardView());
            }
        });

        Label title = new Label("Leaderboard - Top 20");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private VBox buildTable() {
        TableView<ScoreRecord> table = new TableView<>();

        TableColumn<ScoreRecord, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(d -> {
            int idx = table.getItems().indexOf(d.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        rankCol.setPrefWidth(45);

        TableColumn<ScoreRecord, String> playerCol = new TableColumn<>("Student");
        playerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        playerCol.setPrefWidth(160);

        TableColumn<ScoreRecord, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getScore())));
        scoreCol.setPrefWidth(80);

        TableColumn<ScoreRecord, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getGrade() + "/100 (" + d.getValue().getLetterGrade() + ")"));
        gradeCol.setPrefWidth(120);

        TableColumn<ScoreRecord, String> accCol = new TableColumn<>("Correct");
        accCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCorrectAnswers() + "/" + d.getValue().getTotalQuestions()));
        accCol.setPrefWidth(90);

        TableColumn<ScoreRecord, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        subjCol.setPrefWidth(140);

        TableColumn<ScoreRecord, String> dateCol = new TableColumn<>("Played");
        dateCol.setCellValueFactory(d -> {
            String p = d.getValue().getPlayedAt();
            return new SimpleStringProperty(p == null ? "" : p.substring(0, Math.min(16, p.length())).replace("T", " "));
        });
        dateCol.setPrefWidth(150);

        table.getColumns().addAll(rankCol, playerCol, scoreCol, gradeCol, accCol, subjCol, dateCol);
        table.setItems(FXCollections.observableArrayList(DataStore.getInstance().getTopScores(20)));
        table.setPlaceholder(new Label("No scores yet. Play a quiz to be the first!"));

        VBox box = new VBox(table);
        box.setPadding(new Insets(20, 30, 30, 30));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
