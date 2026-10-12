package com.quizmaster.views;

import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Student;
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
 * Teacher view: high-level statistics (incl. pass rate and average grade) plus a
 * full table of every graded attempt.
 */
public class ViewReportsView implements SceneManager.View {

    private final BorderPane root;

    public ViewReportsView() {
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
        back.setOnAction(e -> SceneManager.switchTo(new TeacherDashboardView()));

        Label title = new Label("Reports & Statistics");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private VBox buildBody() {
        DataStore store = DataStore.getInstance();
        List<Student> students = store.getAllStudents();
        List<ScoreRecord> allScores = store.getAllScores();

        int totalUsers = store.getAllUsers().size();
        int totalStudents = students.size();
        int totalQuestions = store.getAllQuestions().size();
        int totalGames = allScores.size();
        double avgGrade = allScores.isEmpty() ? 0 :
                allScores.stream().mapToInt(ScoreRecord::getGrade).average().orElse(0);
        long passes = allScores.stream().filter(ScoreRecord::isPassing).count();
        double passRate = allScores.isEmpty() ? 0 : 100.0 * passes / allScores.size();

        HBox stats = new HBox(16,
                statCard("Students", String.valueOf(totalStudents)),
                statCard("Total Users", String.valueOf(totalUsers)),
                statCard("Questions", String.valueOf(totalQuestions)),
                statCard("Games", String.valueOf(totalGames)),
                statCard("Avg Grade", String.format("%.0f/100", avgGrade)),
                statCard("Pass Rate", String.format("%.0f%%", passRate))
        );
        stats.setAlignment(Pos.CENTER);

        TableView<ScoreRecord> table = new TableView<>();

        TableColumn<ScoreRecord, String> playerCol = new TableColumn<>("Student");
        playerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        playerCol.setPrefWidth(130);

        TableColumn<ScoreRecord, String> dateCol = new TableColumn<>("Played At");
        dateCol.setCellValueFactory(d -> {
            String p = d.getValue().getPlayedAt();
            return new SimpleStringProperty(p == null ? "" : p.substring(0, Math.min(19, p.length())).replace("T", " "));
        });
        dateCol.setPrefWidth(155);

        TableColumn<ScoreRecord, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        subjCol.setPrefWidth(130);

        TableColumn<ScoreRecord, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getGrade() + "/100 (" + d.getValue().getLetterGrade() + ")"));
        gradeCol.setPrefWidth(110);

        TableColumn<ScoreRecord, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResult()));
        resultCol.setPrefWidth(70);

        TableColumn<ScoreRecord, String> scoreCol = new TableColumn<>("Points");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getScore())));
        scoreCol.setPrefWidth(70);

        TableColumn<ScoreRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTime()));
        timeCol.setPrefWidth(70);

        table.getColumns().addAll(playerCol, dateCol, subjCol, gradeCol, resultCol, scoreCol, timeCol);
        table.setItems(FXCollections.observableArrayList(allScores));
        table.setPlaceholder(new Label("No games have been played yet."));

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
        card.setPadding(new Insets(16, 20, 16, 20));
        return card;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
