package com.heronix.talkmodule.service;

import com.heronix.talkmodule.model.domain.EmergencyAlert;
import com.heronix.talkmodule.model.dto.EmergencyAlertDTO;
import com.heronix.talkmodule.model.enums.AlertLevel;
import com.heronix.talkmodule.model.enums.AlertType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import com.heronix.talkmodule.repository.EmergencyAlertRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.AudioClip;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service for managing emergency alerts.
 * Handles creation, display, and acknowledgment of alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final EmergencyAlertRepository alertRepository;
    private final SessionManager sessionManager;

    @Getter
    private final ObservableList<EmergencyAlert> activeAlerts = FXCollections.observableArrayList();

    private Consumer<EmergencyAlert> alertReceivedCallback;

    public void setAlertReceivedCallback(Consumer<EmergencyAlert> callback) {
        this.alertReceivedCallback = callback;
    }

    @Transactional
    public void loadActiveAlerts() {
        List<EmergencyAlert> alerts = alertRepository.findActiveAlerts(LocalDateTime.now());
        Platform.runLater(() -> {
            activeAlerts.clear();
            activeAlerts.addAll(alerts);
        });
        log.info("Loaded {} active alerts", alerts.size());
    }

    @Transactional
    public EmergencyAlert createAlert(String title, String message, String instructions,
                                       AlertLevel level, AlertType type,
                                       boolean requiresAck, boolean playSound) {
        EmergencyAlert alert = EmergencyAlert.builder()
                .alertUuid(UUID.randomUUID().toString())
                .title(title)
                .message(message)
                .instructions(instructions)
                .alertLevel(level)
                .alertType(type)
                .issuedById(sessionManager.getCurrentUserId())
                .issuedByName(sessionManager.getCurrentSession() != null ?
                        sessionManager.getCurrentSession().getFullName() : "System")
                .issuedAt(LocalDateTime.now())
                .requiresAcknowledgment(requiresAck)
                .playSound(playSound)
                .campusWide(true)
                .syncStatus(sessionManager.isConnected() ? SyncStatus.PENDING : SyncStatus.LOCAL_ONLY)
                .build();

        // Set default expiration based on level
        switch (level) {
            case EMERGENCY -> alert.setExpiresAt(LocalDateTime.now().plusHours(4));
            case URGENT -> alert.setExpiresAt(LocalDateTime.now().plusHours(2));
            case HIGH -> alert.setExpiresAt(LocalDateTime.now().plusHours(8));
            default -> alert.setExpiresAt(LocalDateTime.now().plusDays(1));
        }

        alertRepository.save(alert);
        Platform.runLater(() -> activeAlerts.add(0, alert));

        log.info("Created {} alert: {}", level, title);

        // Play alert sound if enabled
        if (playSound) {
            playAlertSound(level);
        }

        return alert;
    }

    @Transactional
    public EmergencyAlert createEmergencyAlert(String title, String message, AlertType type) {
        return createAlert(title, message,
                getDefaultInstructions(type),
                AlertLevel.EMERGENCY, type, true, true);
    }

    @Transactional
    public EmergencyAlert createUrgentAlert(String title, String message, AlertType type) {
        return createAlert(title, message, null, AlertLevel.URGENT, type, false, true);
    }

    @Transactional
    public EmergencyAlert createAnnouncement(String title, String message) {
        return createAlert(title, message, null, AlertLevel.NORMAL, AlertType.ANNOUNCEMENT, false, false);
    }

    @Transactional
    public void acknowledgeAlert(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.acknowledge();
            alertRepository.save(alert);
            log.info("Alert acknowledged: {}", alert.getTitle());
        });
    }

    @Transactional
    public void cancelAlert(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.cancel();
            alertRepository.save(alert);
            Platform.runLater(() -> activeAlerts.remove(alert));
            log.info("Alert cancelled: {}", alert.getTitle());
        });
    }

    @Transactional
    public void issueAllClear() {
        // Cancel all active emergency alerts
        List<EmergencyAlert> emergencies = alertRepository.findActiveEmergencyAlerts(LocalDateTime.now());
        for (EmergencyAlert alert : emergencies) {
            alert.cancel();
            alertRepository.save(alert);
        }

        // Create all-clear alert
        createAlert("ALL CLEAR", "The emergency has ended. Normal operations may resume.",
                "Please return to your normal activities.",
                AlertLevel.HIGH, AlertType.ALL_CLEAR, false, true);

        Platform.runLater(() -> {
            activeAlerts.removeIf(a -> a.getAlertLevel() == AlertLevel.EMERGENCY
                    || a.getAlertLevel() == AlertLevel.URGENT);
        });

        log.info("All clear issued");
    }

    public void receiveAlert(EmergencyAlertDTO dto) {
        // Handle incoming alert from WebSocket
        if (alertRepository.findByAlertUuid(dto.getAlertUuid()).isEmpty()) {
            EmergencyAlert alert = EmergencyAlert.builder()
                    .serverId(dto.getId())
                    .alertUuid(dto.getAlertUuid())
                    .title(dto.getTitle())
                    .message(dto.getMessage())
                    .instructions(dto.getInstructions())
                    .alertLevel(dto.getAlertLevel())
                    .alertType(dto.getAlertType())
                    .issuedById(dto.getIssuedById())
                    .issuedByName(dto.getIssuedByName())
                    .issuedAt(dto.getIssuedAt())
                    .expiresAt(dto.getExpiresAt())
                    .requiresAcknowledgment(dto.isRequiresAcknowledgment())
                    .playSound(dto.isPlaySound())
                    .campusWide(dto.isCampusWide())
                    .syncStatus(SyncStatus.SYNCED)
                    .build();

            alertRepository.save(alert);
            Platform.runLater(() -> activeAlerts.add(0, alert));

            // Play sound
            if (alert.isPlaySound()) {
                playAlertSound(alert.getAlertLevel());
            }

            // Notify callback
            if (alertReceivedCallback != null) {
                Platform.runLater(() -> alertReceivedCallback.accept(alert));
            }
        }
    }

    private void playAlertSound(AlertLevel level) {
        try {
            String soundFile = switch (level) {
                case EMERGENCY -> "/sounds/emergency.wav";
                case URGENT -> "/sounds/urgent.wav";
                default -> "/sounds/notification.wav";
            };

            var resource = getClass().getResource(soundFile);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.play();
            }
        } catch (Exception e) {
            log.debug("Could not play alert sound: {}", e.getMessage());
        }
    }

    private String getDefaultInstructions(AlertType type) {
        return switch (type) {
            case LOCKDOWN -> "Remain in your current location. Lock doors. Stay away from windows. Await further instructions.";
            case FIRE -> "Evacuate the building immediately using the nearest exit. Do not use elevators. Proceed to designated assembly area.";
            case WEATHER -> "Move to designated shelter areas immediately. Stay away from windows and exterior walls.";
            case EVACUATION -> "Evacuate the building immediately using the nearest exit. Proceed to designated assembly area.";
            case SHELTER -> "Move to the nearest interior room. Close all doors. Await further instructions.";
            case MEDICAL -> "If safe, provide assistance. Call emergency services if not already notified.";
            default -> "Follow instructions from emergency personnel.";
        };
    }

    public List<EmergencyAlert> getUnacknowledgedAlerts() {
        return alertRepository.findUnacknowledgedAlerts();
    }

    public long getActiveAlertCount() {
        return alertRepository.countActiveAlerts(LocalDateTime.now());
    }

    public long getActiveEmergencyCount() {
        return alertRepository.countActiveEmergencies();
    }

    public List<EmergencyAlert> getAlertHistory() {
        return alertRepository.findAllOrderByIssuedAtDesc();
    }
}
