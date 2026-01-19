package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.service.ChatService;
import com.heronix.talkmodule.service.NewsManagementService;
import com.heronix.talkmodule.service.AlertService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for analytics dashboard.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final ChatService chatService;
    private final NewsManagementService newsService;
    private final AlertService alertService;

    @FXML private Label totalMessagesLabel;
    @FXML private Label messagesChangeLabel;
    @FXML private Label activeChannelsLabel;
    @FXML private Label channelsChangeLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label usersChangeLabel;
    @FXML private Label storageLabel;
    @FXML private Label storageChangeLabel;

    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private ListView<String> activityListView;

    @FXML private TableView<ChannelStat> channelStatsTable;
    @FXML private TableColumn<ChannelStat, String> channelNameColumn;
    @FXML private TableColumn<ChannelStat, String> messageCountColumn;
    @FXML private TableColumn<ChannelStat, String> memberCountColumn;
    @FXML private TableColumn<ChannelStat, String> lastActivityColumn;

    @FXML private Label publishedNewsLabel;
    @FXML private Label scheduledNewsLabel;
    @FXML private Label urgentAlertsLabel;

    @FXML private Label lastUpdatedLabel;

    private final ObservableList<String> activities = FXCollections.observableArrayList();
    private final ObservableList<ChannelStat> channelStats = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        // Initialize time range combo
        timeRangeCombo.getItems().addAll("Today", "Last 7 Days", "Last 30 Days", "All Time");
        timeRangeCombo.setValue("Today");
        timeRangeCombo.setOnAction(e -> loadData());

        // Setup activity list
        activityListView.setItems(activities);

        // Setup channel stats table
        channelNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        messageCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().messageCount())));
        memberCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().memberCount())));
        lastActivityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().lastActivity()));
        channelStatsTable.setItems(channelStats);

        // Load initial data
        loadData();
    }

    private void loadData() {
        // Load summary stats
        int channelCount = chatService.getChannels().size();
        activeChannelsLabel.setText(String.valueOf(channelCount));
        channelsChangeLabel.setText(channelCount > 0 ? "active" : "no channels");

        // Messages - would come from repository in real app
        totalMessagesLabel.setText("0");
        messagesChangeLabel.setText("+0 today");

        // Users - would come from server in real app
        activeUsersLabel.setText("1");
        usersChangeLabel.setText("online now");

        // Storage
        File dataDir = new File("./data");
        if (dataDir.exists()) {
            long size = calculateDirectorySize(dataDir);
            storageLabel.setText(formatSize(size));
        }

        // Load channel stats
        channelStats.clear();
        chatService.getChannels().forEach(channel -> {
            channelStats.add(new ChannelStat(
                    channel.getName(),
                    channel.getMessageCount(),
                    channel.getMemberCount(),
                    channel.getLastMessageTime() != null ?
                            channel.getLastMessageTime().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : "-"
            ));
        });

        // Load activity
        activities.clear();
        activities.add(LocalDateTime.now().format(TIME_FORMAT) + " - Analytics loaded");
        activities.add(LocalDateTime.now().minusMinutes(5).format(TIME_FORMAT) + " - User logged in");

        // Load news stats
        publishedNewsLabel.setText(String.valueOf(newsService.getNewsItems().size()));
        scheduledNewsLabel.setText(String.valueOf(newsService.getScheduledItems().size()));
        urgentAlertsLabel.setText(String.valueOf(alertService.getActiveAlerts().size()));

        // Update timestamp
        lastUpdatedLabel.setText("Last updated: " + LocalDateTime.now().format(TIME_FORMAT));

        log.info("Analytics data loaded");
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
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleExportReport() {
        log.info("Exporting analytics report...");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Report");
        alert.setHeaderText(null);
        alert.setContentText("Report export feature coming soon");
        alert.showAndWait();
    }

    // Record for channel statistics
    public record ChannelStat(String name, int messageCount, int memberCount, String lastActivity) {}
}
