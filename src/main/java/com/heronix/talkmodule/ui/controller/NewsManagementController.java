package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.model.domain.LocalNewsItem;
import com.heronix.talkmodule.service.NewsManagementService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for news management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsManagementController {

    private final NewsManagementService newsService;

    @FXML private TextField headlineField;
    @FXML private TextArea contentArea;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private CheckBox urgentCheckbox;
    @FXML private CheckBox pinnedCheckbox;
    @FXML private DatePicker publishDatePicker;
    @FXML private DatePicker expiresDatePicker;
    @FXML private ToggleButton showScheduledToggle;
    @FXML private ListView<LocalNewsItem> newsListView;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    @FXML
    public void initialize() {
        // Setup category combo
        categoryCombo.getItems().addAll("General", "Events", "Academic", "Sports", "Administrative", "Emergency");
        categoryCombo.setValue("General");

        // Add existing categories
        categoryCombo.getItems().addAll(newsService.getAllCategories());

        // Setup news list
        newsListView.setItems(newsService.getNewsItems());
        newsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LocalNewsItem news, boolean empty) {
                super.updateItem(news, empty);
                if (empty || news == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String prefix = "";
                    if (news.isUrgent()) prefix = "🔴 ";
                    else if (news.isPinned()) prefix = "📌 ";

                    String time = news.getPublishedAt() != null ?
                            news.getPublishedAt().format(FORMAT) : "";
                    setText(prefix + news.getHeadline() + " [" + time + "]");

                    if (news.isUrgent()) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Toggle between published and scheduled
        showScheduledToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                newsListView.setItems(newsService.getScheduledItems());
            } else {
                newsListView.setItems(newsService.getNewsItems());
            }
        });

        // Load news
        newsService.loadNews();
    }

    @FXML
    private void handlePublishNow() {
        String headline = headlineField.getText().trim();
        String content = contentArea.getText().trim();

        if (headline.isEmpty() || content.isEmpty()) {
            showError("Please enter headline and content");
            return;
        }

        String category = categoryCombo.getValue();
        boolean urgent = urgentCheckbox.isSelected();
        boolean pinned = pinnedCheckbox.isSelected();

        newsService.createNews(headline, content, category, urgent, pinned);

        showInfo("News published successfully!");
        clearForm();
    }

    @FXML
    private void handleSchedule() {
        String headline = headlineField.getText().trim();
        String content = contentArea.getText().trim();

        if (headline.isEmpty() || content.isEmpty()) {
            showError("Please enter headline and content");
            return;
        }

        if (publishDatePicker.getValue() == null) {
            showError("Please select a publish date");
            return;
        }

        String category = categoryCombo.getValue();
        LocalDateTime scheduledAt = publishDatePicker.getValue().atStartOfDay();
        LocalDateTime expiresAt = expiresDatePicker.getValue() != null ?
                expiresDatePicker.getValue().atStartOfDay() : null;

        newsService.scheduleNews(headline, content, category, scheduledAt, expiresAt);

        showInfo("News scheduled for " + publishDatePicker.getValue());
        clearForm();
    }

    @FXML
    private void handleEdit() {
        LocalNewsItem selected = newsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a news item to edit");
            return;
        }

        // Populate form with selected item
        headlineField.setText(selected.getHeadline());
        contentArea.setText(selected.getContent());
        categoryCombo.setValue(selected.getCategory());
        urgentCheckbox.setSelected(selected.isUrgent());
        pinnedCheckbox.setSelected(selected.isPinned());
    }

    @FXML
    private void handleTogglePin() {
        LocalNewsItem selected = newsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a news item");
            return;
        }

        newsService.pinNews(selected.getLocalId(), !selected.isPinned());
        newsService.loadNews();
    }

    @FXML
    private void handleDelete() {
        LocalNewsItem selected = newsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a news item to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete News");
        confirm.setHeaderText("Delete this news item?");
        confirm.setContentText(selected.getHeadline());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                newsService.deactivateNews(selected.getLocalId());
            }
        });
    }

    private void clearForm() {
        headlineField.clear();
        contentArea.clear();
        categoryCombo.setValue("General");
        urgentCheckbox.setSelected(false);
        pinnedCheckbox.setSelected(false);
        publishDatePicker.setValue(null);
        expiresDatePicker.setValue(null);
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
