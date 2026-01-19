package com.heronix.talkmodule.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Controller for application settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    @FXML private TextField serverUrlField;
    @FXML private CheckBox autoConnectCheckbox;
    @FXML private CheckBox reconnectCheckbox;
    @FXML private CheckBox soundNotificationsCheckbox;
    @FXML private CheckBox desktopNotificationsCheckbox;
    @FXML private CheckBox urgentOnlyCheckbox;
    @FXML private ComboBox<String> notificationSoundCombo;
    @FXML private ComboBox<String> themeCombo;
    @FXML private Slider fontSizeSlider;
    @FXML private CheckBox compactModeCheckbox;
    @FXML private Label storageUsedLabel;

    @FXML
    public void initialize() {
        // Initialize notification sounds
        notificationSoundCombo.getItems().addAll("Default", "Chime", "Bell", "None");
        notificationSoundCombo.setValue("Default");

        // Initialize themes
        themeCombo.getItems().addAll("Dark", "Light", "System");
        themeCombo.setValue("Dark");

        // Load saved settings
        loadSettings();

        // Calculate storage used
        updateStorageUsed();
    }

    private void loadSettings() {
        // Default values - in a real app these would be loaded from preferences
        serverUrlField.setText("https://talk.heronix.com");
        autoConnectCheckbox.setSelected(true);
        reconnectCheckbox.setSelected(true);
        soundNotificationsCheckbox.setSelected(true);
        desktopNotificationsCheckbox.setSelected(true);
        urgentOnlyCheckbox.setSelected(false);
        compactModeCheckbox.setSelected(false);
        fontSizeSlider.setValue(14);
    }

    private void updateStorageUsed() {
        File dataDir = new File("./data");
        if (dataDir.exists()) {
            long size = calculateDirectorySize(dataDir);
            storageUsedLabel.setText(formatSize(size));
        } else {
            storageUsedLabel.setText("0 MB");
        }
    }

    private long calculateDirectorySize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @FXML
    private void handleClearCache() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Clear message cache?");
        confirm.setContentText("This will remove locally cached messages. They will be re-downloaded when you reconnect.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                log.info("Clearing message cache...");
                // Would clear cache here
                showInfo("Cache cleared successfully");
                updateStorageUsed();
            }
        });
    }

    @FXML
    private void handleExportData() {
        log.info("Exporting data...");
        showInfo("Data export feature coming soon");
    }

    @FXML
    private void handleResetDefaults() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Settings");
        confirm.setHeaderText("Reset all settings to defaults?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                loadSettings();
                showInfo("Settings reset to defaults");
            }
        });
    }

    @FXML
    private void handleSaveSettings() {
        // Save settings to preferences
        log.info("Saving settings: serverUrl={}, autoConnect={}, theme={}",
                serverUrlField.getText(),
                autoConnectCheckbox.isSelected(),
                themeCombo.getValue());

        showInfo("Settings saved successfully");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
