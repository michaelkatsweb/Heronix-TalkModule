package com.heronix.talkmodule;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Heronix TalkModule - Standalone Offline-First Chat Client
 *
 * A Microsoft Teams-style communication application for administrators and staff.
 * Built with the Heronix philosophy: offline-first, no web dependencies, airgapped capable.
 *
 * Features:
 * - Real-time messaging (channels, DMs)
 * - Admin panel for news/announcements
 * - Emergency alert system
 * - User management
 * - Message moderation
 * - Usage analytics
 * - Works offline with local H2 database
 * - Syncs with Heronix-Talk server when available
 */
@SpringBootApplication
@EnableScheduling
public class HeronixTalkModuleApplication extends Application {

    private static ConfigurableApplicationContext springContext;
    private static String[] savedArgs;

    public static void main(String[] args) {
        savedArgs = args;
        launch(args);
    }

    @Override
    public void init() {
        System.out.println("========================================");
        System.out.println("   Heronix TalkModule Starting...      ");
        System.out.println("   Offline-First Chat Client           ");
        System.out.println("========================================");

        springContext = SpringApplication.run(HeronixTalkModuleApplication.class, savedArgs);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Set application icon if available
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        } catch (Exception e) {
            // Icon not available, continue without it
        }

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        // Check for SSO token from Heronix Hub
        String ssoUsername = attemptSsoLogin();

        if (ssoUsername != null) {
            System.out.println("SSO login successful for: " + ssoUsername + ", skipping login screen");
            showMainApplication(primaryStage, ssoUsername);
        } else {
            showLoginScreen(primaryStage);
        }

        primaryStage.show();
    }

    private String attemptSsoLogin() {
        try {
            String tokenFilePath = System.getProperty("user.home") + "/.heronix/auth/token.jwt";
            Path tokenPath = Paths.get(tokenFilePath);

            if (!Files.exists(tokenPath)) {
                return null;
            }

            String token = Files.readString(tokenPath).trim();
            if (token.isEmpty()) return null;

            // Parse JWT payload (trusted local file from Hub)
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            String username = extractJsonValue(payload, "sub");
            String role = extractJsonValue(payload, "role");

            if (username == null || username.isEmpty()) return null;

            // Check expiration
            String expStr = extractJsonValue(payload, "exp");
            if (expStr != null) {
                long exp = Long.parseLong(expStr);
                if (System.currentTimeMillis() / 1000 > exp) {
                    System.out.println("SSO token has expired");
                    return null;
                }
            }

            // Create an offline session with SSO credentials
            com.heronix.talkmodule.service.SessionManager sessionManager =
                    springContext.getBean(com.heronix.talkmodule.service.SessionManager.class);
            sessionManager.createOfflineSession(username, "");

            System.out.println("SSO authentication successful: " + username + " (role: " + role + ")");
            return username;

        } catch (Exception e) {
            System.out.println("SSO login failed, falling back to login screen: " + e.getMessage());
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd < 0) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') valueEnd++;
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    private void showLoginScreen(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 500);

        String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Heronix TalkModule - Login");
        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.setResizable(false);
    }

    private void showMainApplication(Stage stage, String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 800);

        String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Heronix TalkModule - " + username);
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
