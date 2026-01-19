package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.model.domain.EmergencyAlert;
import com.heronix.talkmodule.model.enums.AlertLevel;
import com.heronix.talkmodule.model.enums.AlertType;
import com.heronix.talkmodule.service.AlertService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Controller for emergency alert management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertManagementController {

    private final AlertService alertService;

    @FXML private ComboBox<AlertLevel> alertLevelCombo;
    @FXML private ComboBox<AlertType> alertTypeCombo;
    @FXML private TextField alertTitleField;
    @FXML private TextArea alertMessageArea;
    @FXML private TextArea alertInstructionsArea;
    @FXML private CheckBox requireAckCheckbox;
    @FXML private CheckBox playSoundCheckbox;
    @FXML private ListView<EmergencyAlert> activeAlertsListView;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    @FXML
    public void initialize() {
        // Populate combo boxes
        alertLevelCombo.getItems().addAll(AlertLevel.values());
        alertLevelCombo.setValue(AlertLevel.URGENT);

        alertTypeCombo.getItems().addAll(AlertType.values());
        alertTypeCombo.setValue(AlertType.ANNOUNCEMENT);

        // Setup alerts list
        activeAlertsListView.setItems(alertService.getActiveAlerts());
        activeAlertsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmergencyAlert alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String time = alert.getIssuedAt() != null ?
                            alert.getIssuedAt().format(FORMAT) : "";
                    setText(String.format("[%s] %s - %s: %s",
                            alert.getAlertLevel(), time,
                            alert.getAlertType(), alert.getTitle()));

                    String style = switch (alert.getAlertLevel()) {
                        case EMERGENCY -> "-fx-text-fill: #D32F2F; -fx-font-weight: bold;";
                        case URGENT -> "-fx-text-fill: #F57C00; -fx-font-weight: bold;";
                        case HIGH -> "-fx-text-fill: #FFA000;";
                        default -> "";
                    };
                    setStyle(style);
                }
            }
        });

        // Auto-fill instructions based on type
        alertTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && alertInstructionsArea.getText().isEmpty()) {
                alertInstructionsArea.setText(getDefaultInstructions(newVal));
            }
        });

        // Load active alerts
        alertService.loadActiveAlerts();
    }

    @FXML
    private void handleIssueEmergency() {
        String title = alertTitleField.getText().trim();
        String message = alertMessageArea.getText().trim();

        if (title.isEmpty() || message.isEmpty()) {
            showError("Please enter title and message");
            return;
        }

        AlertType type = alertTypeCombo.getValue();
        alertService.createEmergencyAlert(title, message, type);

        showInfo("Emergency alert issued!");
        clearForm();
    }

    @FXML
    private void handleIssueAlert() {
        String title = alertTitleField.getText().trim();
        String message = alertMessageArea.getText().trim();
        String instructions = alertInstructionsArea.getText().trim();

        if (title.isEmpty() || message.isEmpty()) {
            showError("Please enter title and message");
            return;
        }

        AlertLevel level = alertLevelCombo.getValue();
        AlertType type = alertTypeCombo.getValue();
        boolean requireAck = requireAckCheckbox.isSelected();
        boolean playSound = playSoundCheckbox.isSelected();

        alertService.createAlert(title, message, instructions.isEmpty() ? null : instructions,
                level, type, requireAck, playSound);

        showInfo("Alert issued successfully!");
        clearForm();
    }

    @FXML
    private void handleAllClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm All Clear");
        confirm.setHeaderText("Issue All Clear?");
        confirm.setContentText("This will cancel all active emergency alerts and notify everyone that the emergency has ended.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                alertService.issueAllClear();
                showInfo("All Clear issued!");
            }
        });
    }

    @FXML
    private void handleCancelAlert() {
        EmergencyAlert selected = activeAlertsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an alert to cancel");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Alert");
        confirm.setHeaderText("Cancel this alert?");
        confirm.setContentText("Alert: " + selected.getTitle());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                alertService.cancelAlert(selected.getLocalId());
            }
        });
    }

    @FXML
    private void handleViewHistory() {
        // Could open a dialog showing historical alerts
        showInfo("Alert history feature - showing " +
                alertService.getAlertHistory().size() + " historical alerts");
    }

    private void clearForm() {
        alertTitleField.clear();
        alertMessageArea.clear();
        alertInstructionsArea.clear();
        alertLevelCombo.setValue(AlertLevel.URGENT);
        alertTypeCombo.setValue(AlertType.ANNOUNCEMENT);
        requireAckCheckbox.setSelected(false);
        playSoundCheckbox.setSelected(true);
    }

    private String getDefaultInstructions(AlertType type) {
        return switch (type) {
            case LOCKDOWN -> "Remain in your current location. Lock doors. Stay away from windows.";
            case FIRE -> "Evacuate immediately using nearest exit. Do not use elevators.";
            case WEATHER -> "Move to designated shelter areas. Stay away from windows.";
            case EVACUATION -> "Evacuate the building immediately. Proceed to assembly area.";
            case SHELTER -> "Move to the nearest interior room. Close all doors.";
            default -> "";
        };
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
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
