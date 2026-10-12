package com.quizmaster.views;

import com.quizmaster.models.Question;
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
 * Teacher view: manage the question bank. Lists existing questions on the left,
 * and an add-question form on the right. The subject is chosen from the fixed
 * list of subjects so every question stays inside one of the four pools.
 */
public class ManageQuestionsView implements SceneManager.View {

    private final BorderPane root;
    private final TableView<Question> table = new TableView<>();

    public ManageQuestionsView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildBody());
        refresh();
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 30, 16, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("logout-btn");
        back.setOnAction(e -> SceneManager.switchTo(new TeacherDashboardView()));

        Label title = new Label("Manage Questions");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private HBox buildBody() {
        TableColumn<Question, String> textCol = new TableColumn<>("Question");
        textCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getText()));
        textCol.setPrefWidth(280);

        TableColumn<Question, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        subjCol.setPrefWidth(140);

        TableColumn<Question, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDifficulty().name()));
        diffCol.setPrefWidth(90);

        table.getColumns().addAll(textCol, subjCol, diffCol);
        table.setPlaceholder(new Label("No questions yet."));

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("danger-btn");
        deleteBtn.setOnAction(e -> handleDelete());

        VBox left = new VBox(10, table, deleteBtn);
        left.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox body = new HBox(left, buildAddForm());
        body.setPadding(new Insets(10));
        return body;
    }

    private VBox buildAddForm() {
        Label heading = new Label("Add New Question");
        heading.getStyleClass().add("section-heading");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Question text");
        textArea.setPrefRowCount(2);
        textArea.setWrapText(true);

        TextField opt1 = new TextField(); opt1.setPromptText("Option 1");
        TextField opt2 = new TextField(); opt2.setPromptText("Option 2");
        TextField opt3 = new TextField(); opt3.setPromptText("Option 3");
        TextField opt4 = new TextField(); opt4.setPromptText("Option 4");

        ComboBox<Integer> correctCombo = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        correctCombo.setPromptText("Correct option (1-4)");
        correctCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> subjectCombo = new ComboBox<>(
                FXCollections.observableArrayList(DataStore.getInstance().getSubjects()));
        subjectCombo.setPromptText("Subject");
        subjectCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Question.Difficulty> difficultyCombo = new ComboBox<>(
                FXCollections.observableArrayList(Question.Difficulty.values()));
        difficultyCombo.setPromptText("Difficulty");
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);

        Button addBtn = new Button("Add Question");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            String text = textArea.getText().trim();
            String subject = subjectCombo.getValue();
            Integer correct = correctCombo.getValue();
            Question.Difficulty diff = difficultyCombo.getValue();
            List<String> options = List.of(opt1.getText(), opt2.getText(),
                    opt3.getText(), opt4.getText());

            if (text.isEmpty() || subject == null || correct == null || diff == null
                    || options.stream().anyMatch(String::isBlank)) {
                SceneManager.showAlert(Alert.AlertType.WARNING, "Missing fields",
                        "Please fill in all fields and pick subject, correct option, and difficulty.");
                return;
            }
            DataStore.getInstance().addQuestion(
                    new Question(text, options, correct - 1, subject, diff));
            textArea.clear(); opt1.clear(); opt2.clear(); opt3.clear(); opt4.clear();
            correctCombo.setValue(null); subjectCombo.setValue(null); difficultyCombo.setValue(null);
            refresh();
        });

        VBox form = new VBox(8, heading, textArea, opt1, opt2, opt3, opt4,
                new Label("Correct option:"), correctCombo,
                new Label("Subject:"), subjectCombo,
                new Label("Difficulty:"), difficultyCombo,
                addBtn);
        form.setPadding(new Insets(20));
        form.setPrefWidth(320);
        form.getStyleClass().add("side-panel");
        return form;
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(DataStore.getInstance().getAllQuestions()));
    }

    private void handleDelete() {
        Question selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "No selection",
                    "Please select a question to delete.");
            return;
        }
        DataStore.getInstance().deleteQuestion(selected.getId());
        refresh();
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
