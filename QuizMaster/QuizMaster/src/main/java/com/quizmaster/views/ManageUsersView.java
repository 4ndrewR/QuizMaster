package com.quizmaster.views;

import com.quizmaster.models.User;
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
 * Teacher view: all users in a TableView, with actions to delete a user or
 * change their role (promote Student -> Teacher, demote Teacher -> Student).
 *
 * Role changes are guarded: you cannot demote yourself, and the system always
 * keeps at least one teacher.
 */
public class ManageUsersView implements SceneManager.View {

    private final BorderPane root;
    private final TableView<User> table = new TableView<>();

    public ManageUsersView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildTable());
        refreshTable();
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 30, 16, 30));
        header.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("logout-btn");
        back.setOnAction(e -> SceneManager.switchTo(new TeacherDashboardView()));

        Label title = new Label("Manage Users");
        title.getStyleClass().add("header-title");

        header.getChildren().addAll(back, title);
        return header;
    }

    private VBox buildTable() {
        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        usernameCol.setPrefWidth(150);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        nameCol.setPrefWidth(200);

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoleDisplay()));
        roleCol.setPrefWidth(110);

        TableColumn<User, String> dobCol = new TableColumn<>("DOB");
        dobCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDob() == null ? "-" : d.getValue().getDob()));
        dobCol.setPrefWidth(110);

        TableColumn<User, String> createdCol = new TableColumn<>("Joined");
        createdCol.setCellValueFactory(d -> {
            String c = d.getValue().getCreatedAt();
            return new SimpleStringProperty(c == null ? "-" : c.substring(0, Math.min(10, c.length())));
        });
        createdCol.setPrefWidth(110);

        table.getColumns().addAll(usernameCol, nameCol, roleCol, dobCol, createdCol);
        table.setPlaceholder(new Label("No users to display."));

        Button promoteBtn = new Button("Make Teacher");
        promoteBtn.getStyleClass().add("primary-btn");
        promoteBtn.setOnAction(e -> handlePromote());

        Button demoteBtn = new Button("Make Student");
        demoteBtn.getStyleClass().add("primary-btn");
        demoteBtn.setOnAction(e -> handleDemote());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("link-btn");
        refreshBtn.setOnAction(e -> refreshTable());

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("danger-btn");
        deleteBtn.setOnAction(e -> handleDelete());

        HBox actions = new HBox(10, promoteBtn, demoteBtn, refreshBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(12, actions, table);
        container.setPadding(new Insets(20, 30, 30, 30));
        VBox.setVgrow(table, Priority.ALWAYS);
        return container;
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(DataStore.getInstance().getAllUsers()));
    }

    private User requireSelection() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "No selection",
                    "Please select a user first.");
        }
        return selected;
    }

    private void handlePromote() {
        User selected = requireSelection();
        if (selected == null) return;
        if ("TEACHER".equals(selected.getRole())) {
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "Already a teacher",
                    selected.getUsername() + " is already a Teacher.");
            return;
        }
        if (DataStore.getInstance().promoteToTeacher(selected.getUsername())) {
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "Promoted",
                    selected.getUsername() + " is now a Teacher.");
            refreshTable();
        }
    }

    private void handleDemote() {
        User selected = requireSelection();
        if (selected == null) return;
        if ("STUDENT".equals(selected.getRole())) {
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "Already a student",
                    selected.getUsername() + " is already a Student.");
            return;
        }
        if (selected.getUsername().equals(AuthService.getCurrentUser().getUsername())) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "Cannot demote yourself",
                    "You can't remove your own teacher role while logged in.");
            return;
        }
        if (DataStore.getInstance().countTeachers() <= 1) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "Last teacher",
                    "There must always be at least one teacher.");
            return;
        }
        if (DataStore.getInstance().demoteToStudent(selected.getUsername())) {
            SceneManager.showAlert(Alert.AlertType.INFORMATION, "Demoted",
                    selected.getUsername() + " is now a Student.");
            refreshTable();
        }
    }

    private void handleDelete() {
        User selected = requireSelection();
        if (selected == null) return;
        if (selected.getUsername().equals(AuthService.getCurrentUser().getUsername())) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "Cannot delete",
                    "You can't delete the currently-logged-in account.");
            return;
        }
        if ("TEACHER".equals(selected.getRole()) && DataStore.getInstance().countTeachers() <= 1) {
            SceneManager.showAlert(Alert.AlertType.WARNING, "Last teacher",
                    "You can't delete the only remaining teacher.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user '" + selected.getUsername() + "'? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                DataStore.getInstance().deleteUser(selected.getUsername());
                refreshTable();
            }
        });
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
