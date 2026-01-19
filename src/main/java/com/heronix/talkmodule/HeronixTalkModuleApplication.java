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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();

        Scene scene = new Scene(root, 400, 500);

        // Load CSS
        String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("Heronix TalkModule - Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setResizable(false);

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

        primaryStage.show();
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
