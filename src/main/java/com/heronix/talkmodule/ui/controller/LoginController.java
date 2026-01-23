package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.HeronixTalkModuleApplication;
import com.heronix.talkmodule.model.dto.AuthResponseDTO;
import com.heronix.talkmodule.network.TalkServerClient;
import com.heronix.talkmodule.service.SessionManager;
import com.heronix.talkmodule.service.WebSocketService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Controller for the login screen.
 * Supports both online and offline modes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final TalkServerClient serverClient;
    private final SessionManager sessionManager;
    private final WebSocketService webSocketService;

    @Value("${heronix.server.url:http://localhost:9680}")
    private String defaultServerUrl;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField serverUrlField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private CheckBox offlineModeCheckbox;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox serverUrlContainer;

    @FXML
    public void initialize() {
        serverUrlField.setText(defaultServerUrl);
        progressIndicator.setVisible(false);
        statusLabel.setText("");

        // Toggle server URL field based on offline mode
        offlineModeCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            serverUrlContainer.setDisable(newVal);
            serverUrlContainer.setOpacity(newVal ? 0.5 : 1.0);
        });

        // Try to load existing session
        Platform.runLater(() -> {
            Optional<com.heronix.talkmodule.model.domain.CurrentSession> existingSession =
                    sessionManager.loadExistingSession();
            if (existingSession.isPresent() && existingSession.get().isRememberMe()) {
                usernameField.setText(existingSession.get().getUsername());
                rememberMeCheckbox.setSelected(true);
                statusLabel.setText("Welcome back! Enter password to continue.");
            }
        });

        // Enter key on password field triggers login
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String serverUrl = serverUrlField.getText().trim();
        boolean offlineMode = offlineModeCheckbox.isSelected();
        boolean rememberMe = rememberMeCheckbox.isSelected();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        setLoading(true);
        statusLabel.setText("Authenticating...");

        // Run authentication in background
        new Thread(() -> {
            try {
                if (offlineMode) {
                    // Offline mode - create local session
                    Platform.runLater(() -> {
                        sessionManager.createOfflineSession(username, password);
                        openMainWindow();
                    });
                } else {
                    // Online mode - authenticate with server
                    serverClient.setServerUrl(serverUrl);

                    if (!serverClient.isServerReachable()) {
                        Platform.runLater(() -> {
                            showError("Cannot connect to server. Try offline mode?");
                            setLoading(false);
                        });
                        return;
                    }

                    Optional<AuthResponseDTO> response = serverClient.login(username, password, "TalkModule");

                    Platform.runLater(() -> {
                        if (response.isPresent() && response.get().isSuccess()) {
                            sessionManager.createSession(response.get(), serverUrl, rememberMe);
                            // Connect WebSocket for real-time messaging
                            connectWebSocket(serverUrl);
                            openMainWindow();
                        } else {
                            String message = response.map(AuthResponseDTO::getMessage)
                                    .orElse("Authentication failed");
                            showError(message);
                            setLoading(false);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Login error", e);
                Platform.runLater(() -> {
                    showError("Login error: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleCheckServer() {
        String serverUrl = serverUrlField.getText().trim();
        if (serverUrl.isEmpty()) {
            showError("Please enter server URL");
            return;
        }

        setLoading(true);
        statusLabel.setText("Checking server...");

        new Thread(() -> {
            serverClient.setServerUrl(serverUrl);
            boolean reachable = serverClient.isServerReachable();

            Platform.runLater(() -> {
                setLoading(false);
                if (reachable) {
                    statusLabel.setText("Server is online and reachable!");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                } else {
                    statusLabel.setText("Server is not reachable");
                    statusLabel.setStyle("-fx-text-fill: #f44336;");
                }
            });
        }).start();
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
            loader.setControllerFactory(HeronixTalkModuleApplication.getSpringContext()::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);

            String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Heronix TalkModule - " + sessionManager.getCurrentUsername());
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (Exception e) {
            log.error("Error opening main window", e);
            showError("Error loading application: " + e.getMessage());
        }
    }

    private void connectWebSocket(String serverUrl) {
        // Connect WebSocket in background - don't block UI
        webSocketService.connect(serverUrl)
                .thenAccept(connected -> {
                    if (connected) {
                        log.info("WebSocket connected successfully");
                    } else {
                        log.warn("WebSocket connection failed, real-time messaging unavailable");
                    }
                })
                .exceptionally(e -> {
                    log.error("WebSocket connection error", e);
                    return null;
                });
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #f44336;");
    }
}
