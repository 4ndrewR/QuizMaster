package com.quizmaster.views;

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
 * Teacher view: overview of every subject — how many questions it holds and how
 * many students are enrolled in it.
 */
public class TeacherSubjectsView implements SceneManager.View {

    private final BorderPane root;

    /** Small row model for the overview table. */
    private static class SubjectStat {
        final String subject;
        final long questions;
        final long enrolled;
        SubjectStat(String subject, long questions, long enrolled) {
            this.subject = subject;
            this.questions = questions;
            this.enrolled = enrolled;
        }
    }

    public TeacherSubjectsView() {
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

        Label title = new Label("Subjects Overview");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private VBox buildBody() {
        DataStore store = DataStore.getInstance();
        List<Student> students = store.getAllStudents();

        TableView<SubjectStat> table = new TableView<>();

        TableColumn<SubjectStat, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().subject));
        subjCol.setPrefWidth(240);

        TableColumn<SubjectStat, String> qCol = new TableColumn<>("Questions");
        qCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().questions)));
        qCol.setPrefWidth(140);

        TableColumn<SubjectStat, String> eCol = new TableColumn<>("Enrolled Students");
        eCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().enrolled)));
        eCol.setPrefWidth(180);

        table.getColumns().addAll(subjCol, qCol, eCol);

        for (String subject : store.getSubjects()) {
            long questions = store.countQuestionsInSubject(subject);
            long enrolled = students.stream().filter(s -> s.isEnrolledIn(subject)).count();
            table.getItems().add(new SubjectStat(subject, questions, enrolled));
        }
        table.setPlaceholder(new Label("No subjects configured."));

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
