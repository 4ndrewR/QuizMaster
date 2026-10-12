package com.quizmaster.views;

import com.quizmaster.models.ScoreRecord;
import com.quizmaster.models.Student;
import com.quizmaster.services.AuthService;
import com.quizmaster.services.DataStore;
import com.quizmaster.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Student view: enroll in or drop subjects. A student can only be quizzed on
 * subjects they are enrolled in. Each row shows the subject's question count
 * and the student's best grade in that subject so far.
 */
public class SubjectsView implements SceneManager.View {

    private final BorderPane root;

    public SubjectsView() {
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

        Label title = new Label("My Subjects");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private ScrollPane buildBody() {
        Student student = (Student) AuthService.getCurrentUser();
        DataStore store = DataStore.getInstance();
        List<ScoreRecord> myScores = store.getScoresForUser(student.getUsername());

        VBox list = new VBox(14);
        list.setPadding(new Insets(24, 30, 30, 30));

        Label intro = new Label("Enroll in a subject to unlock its quizzes. "
                + "You're enrolled in " + student.getEnrolledSubjects().size()
                + " of " + store.getSubjects().size() + " subjects.");
        intro.getStyleClass().add("subtitle");
        intro.setWrapText(true);
        list.getChildren().add(intro);

        for (String subject : store.getSubjects()) {
            boolean enrolled = student.isEnrolledIn(subject);
            long questionCount = store.countQuestionsInSubject(subject);

            int bestGrade = myScores.stream()
                    .filter(s -> subject.equals(s.getSubject()))
                    .mapToInt(ScoreRecord::getGrade)
                    .max().orElse(-1);

            Label name = new Label(subject);
            name.getStyleClass().add("section-heading");

            Label detail = new Label(questionCount + " questions"
                    + "   \u2022   " + (enrolled ? "Enrolled" : "Not enrolled")
                    + (bestGrade < 0 ? "" : "   \u2022   Best grade: " + bestGrade + "/100"));
            detail.getStyleClass().add("tile-sub");

            VBox info = new VBox(4, name, detail);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button toggle = new Button(enrolled ? "Drop" : "Enroll");
            toggle.getStyleClass().add(enrolled ? "danger-btn" : "primary-btn");
            toggle.setOnAction(e -> {
                if (enrolled) student.drop(subject);
                else student.enroll(subject);
                store.updateUser(student);
                root.setCenter(buildBody()); // rebuild to reflect the change
            });

            HBox row = new HBox(14, info, spacer, toggle);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("review-item");
            row.setPadding(new Insets(16));
            list.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("review-scroll");
        return scroll;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
