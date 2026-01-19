package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Controller for user management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final SessionManager sessionManager;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ListView<String> userListView;
    @FXML private Label userCountLabel;

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField departmentField;
    @FXML private CheckBox activeCheckbox;
    @FXML private CheckBox verifiedCheckbox;
    @FXML private Label lastLoginLabel;
    @FXML private Label createdLabel;

    private final ObservableList<String> users = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize role filter
        roleFilterCombo.getItems().addAll("All Roles", "Admin", "Staff", "Student", "Guest");
        roleFilterCombo.setValue("All Roles");

        // Initialize status filter
        statusFilterCombo.getItems().addAll("All Status", "Active", "Inactive", "Pending");
        statusFilterCombo.setValue("All Status");

        // Initialize role combo
        roleCombo.getItems().addAll("Admin", "Staff", "Student", "Guest");

        // Setup user list
        userListView.setItems(users);
        userListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> loadUserDetails(newVal));

        // Load users
        loadUsers();
    }

    private void loadUsers() {
        users.clear();

        if (sessionManager.isConnected()) {
            // Would fetch from server
            log.info("Loading users from server...");
        }

        // Add sample data for demo
        users.addAll(
                "admin - Administrator",
                "jsmith - John Smith (Staff)",
                "mjones - Mary Jones (Staff)",
                "student1 - Test Student"
        );

        userCountLabel.setText(users.size() + " users");
    }

    private void loadUserDetails(String user) {
        if (user == null) {
            clearForm();
            return;
        }

        // Parse the user string and populate form
        String username = user.split(" - ")[0];
        usernameField.setText(username);

        if (username.equals("admin")) {
            fullNameField.setText("System Administrator");
            emailField.setText("admin@heronix.com");
            roleCombo.setValue("Admin");
            departmentField.setText("IT");
            activeCheckbox.setSelected(true);
            verifiedCheckbox.setSelected(true);
            lastLoginLabel.setText("Today");
            createdLabel.setText("2024-01-01");
        } else {
            fullNameField.setText(user.contains("-") ? user.split(" - ")[1].split(" \\(")[0] : "");
            emailField.setText(username + "@heronix.edu");
            roleCombo.setValue("Staff");
            departmentField.setText("General");
            activeCheckbox.setSelected(true);
            verifiedCheckbox.setSelected(true);
            lastLoginLabel.setText("-");
            createdLabel.setText("-");
        }
    }

    private void clearForm() {
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        roleCombo.setValue(null);
        departmentField.clear();
        activeCheckbox.setSelected(false);
        verifiedCheckbox.setSelected(false);
        lastLoginLabel.setText("-");
        createdLabel.setText("-");
    }

    @FXML
    private void handleSearch() {
        String term = searchField.getText().toLowerCase();
        if (term.isEmpty()) {
            loadUsers();
            return;
        }

        users.removeIf(u -> !u.toLowerCase().contains(term));
        userCountLabel.setText(users.size() + " users found");
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleSaveUser() {
        if (usernameField.getText().isEmpty()) {
            showError("Please select a user to edit");
            return;
        }

        log.info("Saving user: {}", usernameField.getText());
        showInfo("User updated successfully");
    }

    @FXML
    private void handleResetPassword() {
        if (usernameField.getText().isEmpty()) {
            showError("Please select a user");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Reset password for " + usernameField.getText() + "?");
        confirm.setContentText("A password reset link will be sent to the user's email.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                log.info("Password reset requested for: {}", usernameField.getText());
                showInfo("Password reset email sent");
            }
        });
    }

    @FXML
    private void handleDisableUser() {
        if (usernameField.getText().isEmpty()) {
            showError("Please select a user");
            return;
        }

        if (usernameField.getText().equals("admin")) {
            showError("Cannot disable the admin account");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Disable User");
        confirm.setHeaderText("Disable user " + usernameField.getText() + "?");
        confirm.setContentText("The user will no longer be able to log in.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                log.info("User disabled: {}", usernameField.getText());
                activeCheckbox.setSelected(false);
                showInfo("User disabled");
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
