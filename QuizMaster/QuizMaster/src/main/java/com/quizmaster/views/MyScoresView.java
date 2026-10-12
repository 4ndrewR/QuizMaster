package com.quizmaster.views;

import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Student;
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

import java.util.List;

/**
 * Student view: personal score history with grades, plus aggregate stats.
 */
public class MyScoresView implements SceneManager.View {

    private final BorderPane root;

    public MyScoresView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildBody());
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 30, 16, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("logout-btn");
        back.setOnAction(e -> SceneManager.switchTo(new StudentDashboardView()));

        Label title = new Label("My Scores");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private VBox buildBody() {
        Student student = (Student) AuthService.getCurrentUser();
        List<ScoreRecord> scores = DataStore.getInstance().getScoresForUser(student.getUsername());

        int bestGrade = DataStore.getInstance().getBestGrade(student.getUsername());
        long passes = scores.stream().filter(ScoreRecord::isPassing).count();

        HBox stats = new HBox(20,
                statCard("Total Points", String.valueOf(student.getTotalPoints())),
                statCard("Games Played", String.valueOf(student.getGamesPlayed())),
                statCard("Best Grade", bestGrade < 0 ? "N/A" : bestGrade + "/100"),
                statCard("Passed", passes + "/" + scores.size())
        );
        stats.setAlignment(Pos.CENTER);

        TableView<ScoreRecord> table = new TableView<>();

        TableColumn<ScoreRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> {
            String p = d.getValue().getPlayedAt();
            return new SimpleStringProperty(p == null ? "" : p.substring(0, Math.min(16, p.length())).replace("T", " "));
        });
        dateCol.setPrefWidth(150);

        TableColumn<ScoreRecord, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        subjCol.setPrefWidth(150);

        TableColumn<ScoreRecord, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getGrade() + "/100 (" + d.getValue().getLetterGrade() + ")"));
        gradeCol.setPrefWidth(120);

        TableColumn<ScoreRecord, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResult()));
        resultCol.setPrefWidth(80);

        TableColumn<ScoreRecord, String> scoreCol = new TableColumn<>("Points");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getScore())));
        scoreCol.setPrefWidth(80);

        TableColumn<ScoreRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTime()));
        timeCol.setPrefWidth(80);

        table.getColumns().addAll(dateCol, subjCol, gradeCol, resultCol, scoreCol, timeCol);
        table.setItems(FXCollections.observableArrayList(scores));
        table.setPlaceholder(new Label("You haven't played any games yet."));

        VBox body = new VBox(20, stats, table);
        body.setPadding(new Insets(20, 30, 30, 30));
        VBox.setVgrow(table, Priority.ALWAYS);
        return body;
    }

    private VBox statCard(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("big-number");
        Label l = new Label(label);
        l.getStyleClass().add("subtitle");
        VBox card = new VBox(4, v, l);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20, 28, 20, 28));
        return card;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
