package com.heronix.talkmodule.ui.controller;

import com.heronix.talkmodule.model.dto.ParentContactDTO;
import com.heronix.talkmodule.model.dto.ParentMessageDTO;
import com.heronix.talkmodule.service.ParentMessageService;
import com.heronix.talkmodule.service.SessionManager;
import com.heronix.talkmodule.service.SisApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Parent Communication Center
 *
 * Provides interface for staff to communicate with parents:
 * - Send individual messages to parents
 * - View message history
 * - Send bulk announcements
 * - Manage message templates
 *
 * @author Heronix TalkModule Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParentCommunicationController {

    private final ParentMessageService parentMessageService;
    private final SessionManager sessionManager;
    private final SisApiClient sisApiClient;

    @FXML private StackPane contentArea;

    // Tab buttons
    @FXML private Button messageTabBtn;
    @FXML private Button historyTabBtn;
    @FXML private Button bulkTabBtn;
    @FXML private Button templatesTabBtn;

    // Panels
    @FXML private VBox messagePanel;
    @FXML private VBox historyPanel;
    @FXML private VBox bulkPanel;
    @FXML private VBox templatesPanel;

    // Status
    @FXML private Label serviceStatusLabel;
    @FXML private Label statusLabel;
    @FXML private Label lastSyncLabel;

    // Message Tab
    @FXML private TextField studentSearchField;
    @FXML private VBox studentInfoCard;
    @FXML private Label studentNameLabel;
    @FXML private Label studentGradeLabel;
    @FXML private Label studentIdLabel;
    @FXML private VBox parentSelectionBox;
    @FXML private ListView<ParentContactDTO> parentListView;
    @FXML private VBox messageCompositionBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea messageContentArea;
    @FXML private CheckBox inAppCheckBox;
    @FXML private CheckBox pushCheckBox;
    @FXML private CheckBox emailCheckBox;
    @FXML private CheckBox smsCheckBox;
    @FXML private CheckBox requireAckCheckBox;
    @FXML private Button sendButton;

    // History Tab
    @FXML private TextField historySearchField;
    @FXML private ComboBox<String> historyFilterComboBox;
    @FXML private TableView<MessageHistoryItem> historyTableView;
    @FXML private TableColumn<MessageHistoryItem, String> historyDateCol;
    @FXML private TableColumn<MessageHistoryItem, String> historyRecipientCol;
    @FXML private TableColumn<MessageHistoryItem, String> historyCategoryCol;
    @FXML private TableColumn<MessageHistoryItem, String> historySubjectCol;
    @FXML private TableColumn<MessageHistoryItem, String> historyStatusCol;
    @FXML private TableColumn<MessageHistoryItem, String> historyActionsCol;

    // Bulk Tab
    @FXML private ComboBox<String> audienceComboBox;
    @FXML private ComboBox<String> gradeLevelComboBox;
    @FXML private Label recipientCountLabel;
    @FXML private TextField bulkSubjectField;
    @FXML private TextArea bulkContentArea;
    @FXML private CheckBox bulkPushCheckBox;
    @FXML private CheckBox bulkEmailCheckBox;
    @FXML private CheckBox scheduleCheckBox;
    @FXML private HBox scheduleBox;
    @FXML private DatePicker scheduleDatePicker;
    @FXML private TextField scheduleTimeField;
    @FXML private Button sendBulkButton;

    // Templates Tab
    @FXML private ListView<MessageTemplate> templateListView;
    @FXML private VBox templatePreviewBox;
    @FXML private Label templateNameLabel;
    @FXML private Label templateCategoryLabel;
    @FXML private TextArea templateContentPreview;

    // Data
    private ObservableList<ParentContactDTO> parentContacts = FXCollections.observableArrayList();
    private ObservableList<MessageHistoryItem> messageHistory = FXCollections.observableArrayList();
    private ObservableList<MessageTemplate> templates = FXCollections.observableArrayList();
    private ParentContactDTO selectedParent;
    private Long selectedStudentId;
    private Runnable onBackAction;

    @FXML
    public void initialize() {
        log.info("Initializing Parent Communication Controller");

        setupMessageTab();
        setupHistoryTab();
        setupBulkTab();
        setupTemplatesTab();

        checkServiceStatus();

        // Default to message tab
        handleMessageTab();

        log.info("Parent Communication Controller initialized");
    }

    private void setupMessageTab() {
        // Setup parent list view
        parentListView.setItems(parentContacts);
        parentListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ParentContactDTO parent, boolean empty) {
                super.updateItem(parent, empty);
                if (empty || parent == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(3);
                    Label nameLabel = new Label(parent.getDisplayLabel());
                    nameLabel.setStyle("-fx-font-weight: bold;");

                    String contactInfo = parent.getEmail() != null ? parent.getEmail() : "";
                    if (parent.getCellPhone() != null) {
                        contactInfo += (contactInfo.isEmpty() ? "" : " | ") + parent.getCellPhone();
                    }
                    Label contactLabel = new Label(contactInfo);
                    contactLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                    HBox badges = new HBox(5);
                    if (parent.getPriority() == 1) {
                        Label primary = new Label("Primary");
                        primary.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3;");
                        badges.getChildren().add(primary);
                    }
                    if (parent.isEmergencyContact()) {
                        Label emergency = new Label("Emergency");
                        emergency.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3;");
                        badges.getChildren().add(emergency);
                    }

                    container.getChildren().addAll(nameLabel, contactLabel);
                    if (!badges.getChildren().isEmpty()) {
                        container.getChildren().add(badges);
                    }
                    setGraphic(container);
                }
            }
        });

        parentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedParent = newVal;
            if (newVal != null) {
                messageCompositionBox.setVisible(true);
                messageCompositionBox.setManaged(true);
            }
        });

        // Setup category and priority defaults
        categoryComboBox.setValue("General");
        priorityComboBox.setValue("Normal");

        // Student search on enter
        studentSearchField.setOnAction(e -> handleStudentSearch());
    }

    private void setupHistoryTab() {
        historyTableView.setItems(messageHistory);

        historyDateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTimestamp()));
        historyRecipientCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRecipientName()));
        historyCategoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));
        historySubjectCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSubject()));
        historyStatusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        historyFilterComboBox.setValue("All Categories");
    }

    private void setupBulkTab() {
        audienceComboBox.setValue("All Parents");
        gradeLevelComboBox.setValue("All Grades");

        scheduleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            scheduleBox.setVisible(newVal);
            scheduleBox.setManaged(newVal);
        });

        audienceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRecipientCount();
        });
        gradeLevelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRecipientCount();
        });

        updateRecipientCount();
    }

    private void setupTemplatesTab() {
        templateListView.setItems(templates);
        templateListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MessageTemplate template, boolean empty) {
                super.updateItem(template, empty);
                if (empty || template == null) {
                    setText(null);
                } else {
                    setText(template.getName() + " (" + template.getCategory() + ")");
                }
            }
        });

        templateListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showTemplatePreview(newVal);
            }
        });

        loadTemplates();
    }

    private void checkServiceStatus() {
        parentMessageService.getServiceStatus().thenAccept(status -> {
            Platform.runLater(() -> {
                if (status.isAvailable()) {
                    serviceStatusLabel.setText("● Service Available");
                    serviceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                } else {
                    serviceStatusLabel.setText("● Service Unavailable");
                    serviceStatusLabel.setStyle("-fx-text-fill: #f44336;");
                }
            });
        });
    }

    // ========================================================================
    // TAB HANDLERS
    // ========================================================================

    @FXML
    private void handleMessageTab() {
        switchTab(messageTabBtn, messagePanel);
    }

    @FXML
    private void handleHistoryTab() {
        switchTab(historyTabBtn, historyPanel);
        loadMessageHistory();
    }

    @FXML
    private void handleBulkTab() {
        switchTab(bulkTabBtn, bulkPanel);
    }

    @FXML
    private void handleTemplatesTab() {
        switchTab(templatesTabBtn, templatesPanel);
    }

    private void switchTab(Button activeButton, VBox activePanel) {
        // Update button styles
        messageTabBtn.getStyleClass().remove("active");
        historyTabBtn.getStyleClass().remove("active");
        bulkTabBtn.getStyleClass().remove("active");
        templatesTabBtn.getStyleClass().remove("active");
        activeButton.getStyleClass().add("active");

        // Show/hide panels
        messagePanel.setVisible(false);
        messagePanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        bulkPanel.setVisible(false);
        bulkPanel.setManaged(false);
        templatesPanel.setVisible(false);
        templatesPanel.setManaged(false);

        activePanel.setVisible(true);
        activePanel.setManaged(true);
    }

    // ========================================================================
    // MESSAGE TAB HANDLERS
    // ========================================================================

    @FXML
    private void handleStudentSearch() {
        String searchTerm = studentSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Search Required", "Please enter a student name or ID to search.");
            return;
        }

        setStatus("Searching for student...");

        CompletableFuture.runAsync(() -> {
            List<java.util.Map<String, Object>> results = sisApiClient.searchStudents(searchTerm);
            Platform.runLater(() -> {
                if (results.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "No Results", "No students found matching: " + searchTerm);
                    setStatus("No students found");
                    return;
                }
                java.util.Map<String, Object> student = results.get(0);
                selectedStudentId = student.get("id") instanceof Number ? ((Number) student.get("id")).longValue() : null;
                String firstName = String.valueOf(student.getOrDefault("firstName", ""));
                String lastName = String.valueOf(student.getOrDefault("lastName", ""));
                studentNameLabel.setText(firstName + " " + lastName);
                studentGradeLabel.setText("Grade: " + student.getOrDefault("gradeLevel", ""));
                studentIdLabel.setText("ID: " + student.getOrDefault("studentId", ""));

                studentInfoCard.setVisible(true);
                studentInfoCard.setManaged(true);

                loadParentsForStudent(selectedStudentId);
                setStatus("Student found");
            });
        });
    }

    private void loadParentsForStudent(Long studentId) {
        parentContacts.clear();

        CompletableFuture.runAsync(() -> {
            List<java.util.Map<String, Object>> contacts = sisApiClient.getParentContacts(studentId);
            List<ParentContactDTO> dtos = new ArrayList<>();
            int priority = 1;
            for (java.util.Map<String, Object> c : contacts) {
                dtos.add(ParentContactDTO.builder()
                        .id(c.get("id") instanceof Number ? ((Number) c.get("id")).longValue() : null)
                        .parentToken(String.valueOf(c.getOrDefault("parentToken", "")))
                        .firstName(String.valueOf(c.getOrDefault("firstName", "")))
                        .lastName(String.valueOf(c.getOrDefault("lastName", "")))
                        .relationshipType(String.valueOf(c.getOrDefault("relationshipType", "")))
                        .email(String.valueOf(c.getOrDefault("email", "")))
                        .cellPhone(String.valueOf(c.getOrDefault("cellPhone", "")))
                        .preferredContactMethod(String.valueOf(c.getOrDefault("preferredContactMethod", "EMAIL")))
                        .hasLegalCustody(Boolean.TRUE.equals(c.get("hasLegalCustody")))
                        .canPickUp(Boolean.TRUE.equals(c.get("canPickUp")))
                        .emergencyContact(Boolean.TRUE.equals(c.get("emergencyContact")))
                        .receiveSchoolCommunication(Boolean.TRUE.equals(c.get("receiveSchoolCommunication")))
                        .priority(priority)
                        .studentId(studentId)
                        .studentName(studentNameLabel.getText())
                        .build());
            }
            Platform.runLater(() -> {
                parentContacts.addAll(dtos);
                parentSelectionBox.setVisible(true);
                parentSelectionBox.setManaged(true);
            });
        });
    }

    @FXML
    private void handleSendMessage() {
        if (selectedParent == null) {
            showAlert(Alert.AlertType.WARNING, "No Recipient", "Please select a parent/guardian to send the message to.");
            return;
        }

        String subject = subjectField.getText().trim();
        String content = messageContentArea.getText().trim();

        if (subject.isEmpty() || content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both subject and message content.");
            return;
        }

        sendButton.setDisable(true);
        setStatus("Sending message...");

        ParentMessageDTO message = ParentMessageDTO.builder()
                .studentId(selectedStudentId)
                .parentToken(selectedParent.getParentToken())
                .category(categoryComboBox.getValue().toUpperCase())
                .priority(priorityComboBox.getValue().toUpperCase())
                .subject(subject)
                .content(content)
                .requiresAcknowledgment(requireAckCheckBox.isSelected())
                .deliveryOptions(ParentMessageDTO.DeliveryOptions.builder()
                        .inApp(inAppCheckBox.isSelected())
                        .pushNotification(pushCheckBox.isSelected())
                        .email(emailCheckBox.isSelected())
                        .sms(smsCheckBox.isSelected())
                        .build())
                .build();

        parentMessageService.sendMessage(message).thenAccept(response -> {
            Platform.runLater(() -> {
                sendButton.setDisable(false);
                if (response.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Message Sent",
                            "Your message has been sent to " + selectedParent.getFullName());
                    clearMessageForm();
                    setStatus("Message sent successfully");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Send Failed",
                            "Failed to send message: " + response.getMessage());
                    setStatus("Failed to send message");
                }
            });
        });
    }

    @FXML
    private void handleSaveDraft() {
        String subject = subjectField.getText().trim();
        String content = messageContentArea.getText().trim();
        if (subject.isEmpty() && content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nothing to Save", "Please enter some content before saving a draft.");
            return;
        }
        // Store draft in memory for this session
        draftSubject = subject;
        draftContent = content;
        draftCategory = categoryComboBox.getValue();
        showAlert(Alert.AlertType.INFORMATION, "Draft Saved", "Your message draft has been saved for this session.");
        setStatus("Draft saved");
    }

    private String draftSubject = "";
    private String draftContent = "";
    private String draftCategory = "General";

    @FXML
    private void handlePreview() {
        String subject = subjectField.getText().trim();
        String content = messageContentArea.getText().trim();

        if (subject.isEmpty() && content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nothing to Preview", "Please enter message content first.");
            return;
        }

        Alert preview = new Alert(Alert.AlertType.INFORMATION);
        preview.setTitle("Message Preview");
        preview.setHeaderText("Subject: " + subject);
        preview.setContentText(content);
        preview.getDialogPane().setMinWidth(400);
        preview.showAndWait();
    }

    private void clearMessageForm() {
        subjectField.clear();
        messageContentArea.clear();
        categoryComboBox.setValue("General");
        priorityComboBox.setValue("Normal");
        inAppCheckBox.setSelected(true);
        pushCheckBox.setSelected(false);
        emailCheckBox.setSelected(false);
        smsCheckBox.setSelected(false);
        requireAckCheckBox.setSelected(false);
    }

    // ========================================================================
    // HISTORY TAB HANDLERS
    // ========================================================================

    @FXML
    private void handleRefreshHistory() {
        loadMessageHistory();
    }

    private void loadMessageHistory() {
        setStatus("Loading message history...");
        messageHistory.clear();

        CompletableFuture.runAsync(() -> {
            List<java.util.Map<String, Object>> history = sisApiClient.getMessageHistory(
                    selectedStudentId != null ? selectedStudentId : 0L);
            Platform.runLater(() -> {
                if (history.isEmpty()) {
                    setStatus("No message history found");
                    return;
                }
                for (java.util.Map<String, Object> msg : history) {
                    messageHistory.add(new MessageHistoryItem(
                            String.valueOf(msg.getOrDefault("sentAt", "")),
                            String.valueOf(msg.getOrDefault("recipient", "")),
                            String.valueOf(msg.getOrDefault("category", "")),
                            String.valueOf(msg.getOrDefault("subject", "")),
                            String.valueOf(msg.getOrDefault("status", ""))
                    ));
                }
                setStatus("History loaded: " + history.size() + " messages");
            });
        });
    }

    // ========================================================================
    // BULK TAB HANDLERS
    // ========================================================================

    private void updateRecipientCount() {
        String audience = audienceComboBox.getValue();
        String grade = gradeLevelComboBox.getValue();

        // Calculate based on audience selection
        int count;
        if ("All Parents".equals(audience)) {
            count = parentContacts.size() > 0 ? parentContacts.size() : 0;
        } else if ("By Grade Level".equals(audience) && grade != null && !"All Grades".equals(grade)) {
            count = parentContacts.size() > 0 ? parentContacts.size() : 0;
        } else {
            count = parentContacts.size();
        }

        recipientCountLabel.setText("Estimated recipients: " + count);
    }

    @FXML
    private void handleBulkPreview() {
        String subject = bulkSubjectField.getText().trim();
        String content = bulkContentArea.getText().trim();

        if (subject.isEmpty() && content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nothing to Preview", "Please enter announcement content first.");
            return;
        }

        Alert preview = new Alert(Alert.AlertType.INFORMATION);
        preview.setTitle("Announcement Preview");
        preview.setHeaderText("Subject: " + subject);
        preview.setContentText(content + "\n\n[Will be sent to: " + recipientCountLabel.getText() + "]");
        preview.getDialogPane().setMinWidth(400);
        preview.showAndWait();
    }

    @FXML
    private void handleSendBulk() {
        String subject = bulkSubjectField.getText().trim();
        String content = bulkContentArea.getText().trim();

        if (subject.isEmpty() || content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both subject and announcement content.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bulk Send");
        confirm.setHeaderText("Send Announcement?");
        confirm.setContentText("This will send the announcement to " + recipientCountLabel.getText().replace("Estimated recipients: ", "") + " parents.\n\nAre you sure?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendBulkButton.setDisable(true);
                setStatus("Sending bulk announcement...");

                List<String> parentTokens = new ArrayList<>();
                for (ParentContactDTO pc : parentContacts) {
                    if (pc.getParentToken() != null && !pc.getParentToken().isEmpty()) {
                        parentTokens.add(pc.getParentToken());
                    }
                }

                ParentMessageDTO.Announcement announcement = ParentMessageDTO.Announcement.builder()
                        .subject(subject)
                        .content(content)
                        .category("ANNOUNCEMENT")
                        .parentTokens(parentTokens)
                        .effectiveDate(LocalDateTime.now())
                        .build();

                parentMessageService.sendAnnouncement(announcement).thenAccept(result -> {
                    Platform.runLater(() -> {
                        sendBulkButton.setDisable(false);
                        if (result.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION, "Announcement Sent",
                                    "Successfully sent to " + result.getSuccessCount() + " recipients.");
                            bulkSubjectField.clear();
                            bulkContentArea.clear();
                            setStatus("Announcement sent successfully");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Send Failed",
                                    "Failed to send announcement: " + result.getMessage());
                            setStatus("Failed to send announcement");
                        }
                    });
                });
            }
        });
    }

    // ========================================================================
    // TEMPLATES TAB HANDLERS
    // ========================================================================

    private void loadTemplates() {
        templates.clear();
        // Demo templates
        templates.addAll(
                new MessageTemplate("Absence Follow-up", "Attendance",
                        "Dear Parent,\n\nWe noticed that your child was absent from school today. " +
                        "Please provide a reason for the absence at your earliest convenience.\n\n" +
                        "Thank you for your cooperation."),
                new MessageTemplate("Grade Alert", "Grades",
                        "Dear Parent,\n\nThis is to inform you that your child's grade in [SUBJECT] " +
                        "has dropped below [THRESHOLD]. We recommend scheduling a meeting to discuss strategies for improvement.\n\n" +
                        "Best regards"),
                new MessageTemplate("Behavior Report", "Behavior",
                        "Dear Parent,\n\nWe would like to bring to your attention a behavior incident " +
                        "involving your child today. [DETAILS]\n\nPlease contact us to discuss this matter further."),
                new MessageTemplate("Event Reminder", "Calendar",
                        "Dear Parent,\n\nThis is a reminder about the upcoming [EVENT] scheduled for [DATE].\n\n" +
                        "We look forward to your participation.")
        );
    }

    @FXML
    private void handleCreateTemplate() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Template");
        dialog.setHeaderText("Create a new message template");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(450);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));
        TextField nameField = new TextField();
        nameField.setPromptText("Template name");
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "General", "Attendance", "Grades", "Behavior", "Calendar"));
        catBox.setValue("General");
        TextArea contentField = new TextArea();
        contentField.setPromptText("Template content...");
        contentField.setPrefRowCount(5);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("Content:"), 0, 2);
        grid.add(contentField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                templates.add(new MessageTemplate(
                        nameField.getText().trim(), catBox.getValue(), contentField.getText()));
                setStatus("Template created: " + nameField.getText().trim());
            }
        });
    }

    private void showTemplatePreview(MessageTemplate template) {
        templatePreviewBox.setVisible(true);
        templatePreviewBox.setManaged(true);
        templateNameLabel.setText(template.getName());
        templateCategoryLabel.setText("Category: " + template.getCategory());
        templateContentPreview.setText(template.getContent());
    }

    @FXML
    private void handleEditTemplate() {
        MessageTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Template");
        dialog.setHeaderText("Edit template: " + selected.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(450);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));
        TextField nameField = new TextField(selected.getName());
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "General", "Attendance", "Grades", "Behavior", "Calendar"));
        catBox.setValue(selected.getCategory());
        TextArea contentField = new TextArea(selected.getContent());
        contentField.setPrefRowCount(5);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("Content:"), 0, 2);
        grid.add(contentField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                int idx = templates.indexOf(selected);
                templates.set(idx, new MessageTemplate(
                        nameField.getText().trim(), catBox.getValue(), contentField.getText()));
                setStatus("Template updated: " + nameField.getText().trim());
            }
        });
    }

    @FXML
    private void handleUseTemplate() {
        MessageTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Switch to message tab and populate with template
            handleMessageTab();
            categoryComboBox.setValue(selected.getCategory());
            messageContentArea.setText(selected.getContent());
            setStatus("Template loaded - edit as needed");
        }
    }

    @FXML
    private void handleDeleteTemplate() {
        MessageTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Template");
            confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
            confirm.setContentText("This action cannot be undone.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    templates.remove(selected);
                    templatePreviewBox.setVisible(false);
                    templatePreviewBox.setManaged(false);
                }
            });
        }
    }

    // ========================================================================
    // NAVIGATION
    // ========================================================================

    @FXML
    private void handleBack() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    public void setOnBackAction(Runnable action) {
        this.onBackAction = action;
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        log.debug("Status: {}", message);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Message history item for table display
     */
    public static class MessageHistoryItem {
        private final String timestamp;
        private final String recipientName;
        private final String category;
        private final String subject;
        private final String status;

        public MessageHistoryItem(String timestamp, String recipientName, String category, String subject, String status) {
            this.timestamp = timestamp;
            this.recipientName = recipientName;
            this.category = category;
            this.subject = subject;
            this.status = status;
        }

        public String getTimestamp() { return timestamp; }
        public String getRecipientName() { return recipientName; }
        public String getCategory() { return category; }
        public String getSubject() { return subject; }
        public String getStatus() { return status; }
    }

    /**
     * Message template
     */
    public static class MessageTemplate {
        private final String name;
        private final String category;
        private final String content;

        public MessageTemplate(String name, String category, String content) {
            this.name = name;
            this.category = category;
            this.content = content;
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
    }
}
