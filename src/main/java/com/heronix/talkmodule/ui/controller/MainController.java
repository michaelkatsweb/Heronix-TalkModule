package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.HeronixTalkModuleApplication;
import com.heronix.talkmodule.model.domain.EmergencyAlert;
import com.heronix.talkmodule.model.domain.LocalChannel;
import com.heronix.talkmodule.model.domain.LocalMessage;
import com.heronix.talkmodule.model.enums.ConnectionMode;
import com.heronix.talkmodule.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Main application controller.
 * Manages the primary UI and navigation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final AlertService alertService;
    private final NewsManagementService newsService;

    @FXML private Label userNameLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label unreadCountLabel;

    @FXML private ListView<LocalChannel> channelListView;
    @FXML private ListView<LocalMessage> messageListView;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    @FXML private VBox adminPanel;
    @FXML private Button alertButton;
    @FXML private Button newsButton;
    @FXML private Button usersButton;
    @FXML private Button analyticsButton;

    @FXML private HBox alertBanner;
    @FXML private Label alertBannerText;

    @FXML private Label selectedChannelLabel;
    @FXML private Label memberCountLabel;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        setupUserInfo();
        setupConnectionStatus();
        setupChannelList();
        setupMessageList();
        setupAdminPanel();
        setupAlertBanner();

        // Load initial data
        Platform.runLater(() -> {
            chatService.loadChannels();
            alertService.loadActiveAlerts();
            newsService.loadNews();
            updateUnreadCount();
        });

        // Setup message input
        messageInputArea.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && event.isControlDown()) {
                handleSendMessage();
            }
        });
    }

    private void setupUserInfo() {
        if (sessionManager.getCurrentSession() != null) {
            userNameLabel.setText(sessionManager.getCurrentSession().getFullName());
        }
    }

    private void setupConnectionStatus() {
        sessionManager.getConnectionMode().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateConnectionStatus(newVal));
        });
        updateConnectionStatus(sessionManager.getConnectionMode().get());
    }

    private void updateConnectionStatus(ConnectionMode mode) {
        switch (mode) {
            case CONNECTED -> {
                connectionStatusLabel.setText("● Connected");
                connectionStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
            }
            case OFFLINE -> {
                connectionStatusLabel.setText("○ Offline");
                connectionStatusLabel.setStyle("-fx-text-fill: #9E9E9E;");
            }
            case SYNCING -> {
                connectionStatusLabel.setText("◐ Syncing...");
                connectionStatusLabel.setStyle("-fx-text-fill: #FFC107;");
            }
            case DISCONNECTED -> {
                connectionStatusLabel.setText("● Disconnected");
                connectionStatusLabel.setStyle("-fx-text-fill: #f44336;");
            }
        }
    }

    private void setupChannelList() {
        channelListView.setItems(chatService.getChannels());
        channelListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LocalChannel channel, boolean empty) {
                super.updateItem(channel, empty);
                if (empty || channel == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String prefix = channel.isDirectMessage() ? "👤 " :
                            (channel.isPublic() ? "# " : "🔒 ");
                    String unread = channel.getUnreadCount() > 0 ?
                            " (" + channel.getUnreadCount() + ")" : "";
                    setText(prefix + channel.getName() + unread);

                    if (channel.getUnreadCount() > 0) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        channelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chatService.selectChannel(newVal);
                selectedChannelLabel.setText(newVal.getName());
                memberCountLabel.setText(newVal.getMemberCount() + " members");
            }
        });
    }

    private void setupMessageList() {
        messageListView.setItems(chatService.getCurrentMessages());
        messageListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LocalMessage message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    String time = message.getTimestamp() != null ?
                            message.getTimestamp().format(TIME_FORMAT) : "";
                    setText(String.format("[%s] %s: %s",
                            time, message.getSenderName(), message.getContent()));
                }
            }
        });

        // Auto-scroll to bottom on new messages
        chatService.getCurrentMessages().addListener(
                (javafx.collections.ListChangeListener<LocalMessage>) c -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            Platform.runLater(() ->
                                    messageListView.scrollTo(messageListView.getItems().size() - 1));
                        }
                    }
                });
    }

    private void setupAdminPanel() {
        // Show admin panel only for admins
        boolean isAdmin = sessionManager.isAdmin();
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
    }

    private void setupAlertBanner() {
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);

        // Listen for new alerts
        alertService.setAlertReceivedCallback(this::showAlertBanner);

        // Check for existing alerts
        alertService.getActiveAlerts().addListener(
                (javafx.collections.ListChangeListener<EmergencyAlert>) c -> {
                    while (c.next()) {
                        if (c.wasAdded() && !c.getAddedSubList().isEmpty()) {
                            Platform.runLater(() ->
                                    showAlertBanner(c.getAddedSubList().get(0)));
                        }
                    }
                });
    }

    private void showAlertBanner(EmergencyAlert alert) {
        // Show persistent banner
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);
        alertBannerText.setText("⚠ " + alert.getTitle() + ": " + alert.getMessage());

        String style = switch (alert.getAlertLevel()) {
            case EMERGENCY -> "-fx-background-color: #D32F2F;";
            case URGENT -> "-fx-background-color: #F57C00;";
            case HIGH -> "-fx-background-color: #FFA000;";
            default -> "-fx-background-color: #1976D2;";
        };
        alertBanner.setStyle(style);

        // For critical alerts (EMERGENCY or URGENT), show a modal dialog
        if (alert.getAlertLevel() == com.heronix.talkmodule.model.enums.AlertLevel.EMERGENCY ||
            alert.getAlertLevel() == com.heronix.talkmodule.model.enums.AlertLevel.URGENT) {
            showCriticalAlertDialog(alert);
        }
    }

    /**
     * Show a modal dialog for critical emergency alerts
     */
    private void showCriticalAlertDialog(EmergencyAlert alert) {
        Platform.runLater(() -> {
            Alert.AlertType alertType = alert.getAlertLevel() == com.heronix.talkmodule.model.enums.AlertLevel.EMERGENCY
                    ? Alert.AlertType.ERROR
                    : Alert.AlertType.WARNING;

            Alert alertDialog = new Alert(alertType);
            alertDialog.setTitle(alert.getAlertLevel() + " ALERT");
            alertDialog.setHeaderText(getAlertIcon(alert.getAlertType()) + " " + alert.getTitle());

            // Build content text
            StringBuilder content = new StringBuilder();
            content.append(alert.getMessage());

            if (alert.getInstructions() != null && !alert.getInstructions().isEmpty()) {
                content.append("\n\n📋 Instructions:\n").append(alert.getInstructions());
            }
            if (alert.getIssuedByName() != null) {
                content.append("\n\nIssued by: ").append(alert.getIssuedByName());
            }

            alertDialog.setContentText(content.toString());

            // Style the dialog for critical alerts
            alertDialog.getDialogPane().setStyle(
                    "-fx-background-color: #ffebee; " +
                    "-fx-border-color: " + (alert.getAlertLevel() == com.heronix.talkmodule.model.enums.AlertLevel.EMERGENCY ? "#f44336" : "#ff9800") + "; " +
                    "-fx-border-width: 3px;"
            );

            // Add acknowledge button if required
            if (alert.isRequiresAcknowledgment()) {
                ButtonType acknowledgeBtn = new ButtonType("Acknowledge", ButtonBar.ButtonData.OK_DONE);
                alertDialog.getButtonTypes().setAll(acknowledgeBtn);

                alertDialog.showAndWait().ifPresent(response -> {
                    if (response == acknowledgeBtn && alert.getLocalId() != null) {
                        alertService.acknowledgeAlert(alert.getLocalId());
                    }
                });
            } else {
                alertDialog.show();
            }
        });
    }

    /**
     * Get icon for alert type
     */
    private String getAlertIcon(com.heronix.talkmodule.model.enums.AlertType alertType) {
        if (alertType == null) return "⚠️";
        return switch (alertType) {
            case LOCKDOWN -> "🔒";
            case FIRE -> "🔥";
            case WEATHER -> "🌪️";
            case MEDICAL -> "🏥";
            case EVACUATION -> "🚨";
            case SHELTER -> "🏠";
            case ALL_CLEAR -> "✅";
            case ANNOUNCEMENT -> "📢";
            case SCHEDULE_CHANGE -> "📅";
            default -> "⚠️";
        };
    }

    @FXML
    private void handleDismissAlert() {
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInputArea.getText().trim();
        if (content.isEmpty()) return;

        LocalChannel channel = chatService.getSelectedChannel();
        if (channel == null) {
            showAlert("Please select a channel first");
            return;
        }

        chatService.sendMessage(channel.getId(), content);
        messageInputArea.clear();
    }

    @FXML
    private void handleCreateChannel() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Channel");
        dialog.setHeaderText("Enter channel name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                chatService.createChannel(name.trim(), "", "chat");
            }
        });
    }

    @FXML
    private void handleOpenAlerts() {
        openWindow("/fxml/AlertManagement.fxml", "Emergency Alerts", 800, 600);
    }

    @FXML
    private void handleOpenNews() {
        openWindow("/fxml/NewsManagement.fxml", "News Management", 900, 700);
    }

    @FXML
    private void handleOpenUsers() {
        openWindow("/fxml/UserManagement.fxml", "User Management", 900, 700);
    }

    @FXML
    private void handleOpenAnalytics() {
        openWindow("/fxml/Analytics.fxml", "Usage Analytics", 1000, 700);
    }

    @FXML
    private void handleOpenSettings() {
        openWindow("/fxml/Settings.fxml", "Settings", 600, 500);
    }

    @FXML
    private void handleLogout() {
        sessionManager.clearSession();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            loader.setControllerFactory(HeronixTalkModuleApplication.getSpringContext()::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 500);

            String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setTitle("Heronix TalkModule - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (Exception e) {
            log.error("Error returning to login", e);
        }
    }

    private void openWindow(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(HeronixTalkModuleApplication.getSpringContext()::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);

            Stage stage = new Stage();
            stage.setTitle("Heronix TalkModule - " + title);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            log.error("Error opening window: {}", fxmlPath, e);
            showAlert("Error opening " + title);
        }
    }

    private void updateUnreadCount() {
        long count = chatService.getTotalUnreadCount();
        unreadCountLabel.setText(count > 0 ? String.valueOf(count) : "");
        unreadCountLabel.setVisible(count > 0);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
