package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.AlertLevel;
import com.heronix.talkmodule.model.enums.AlertType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emergency alert entity for campus-wide notifications.
 */
@Entity
@Table(name = "emergency_alerts", indexes = {
        @Index(name = "idx_alert_level", columnList = "alertLevel"),
        @Index(name = "idx_alert_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long localId;

    private Long serverId;

    @Column(unique = true, nullable = false)
    private String alertUuid;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    private Long issuedById;
    private String issuedByName;

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime cancelledAt;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean acknowledged = false;

    @Builder.Default
    private boolean requiresAcknowledgment = false;

    // Audio alert settings
    @Builder.Default
    private boolean playSound = true;

    private String soundFile;

    @Builder.Default
    private int repeatCount = 3;  // How many times to repeat audio

    // Target audience
    private String targetRoles;  // JSON array of roles
    private String targetDepartments;  // JSON array of departments

    @Builder.Default
    private boolean campusWide = true;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;

    private LocalDateTime lastSyncTime;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        if (alertUuid == null) {
            alertUuid = UUID.randomUUID().toString();
        }
        createdDate = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean isActiveAlert() {
        return active && !isExpired() && !isCancelled();
    }

    public boolean isEmergency() {
        return alertLevel == AlertLevel.EMERGENCY;
    }

    public boolean isUrgent() {
        return alertLevel == AlertLevel.EMERGENCY || alertLevel == AlertLevel.URGENT;
    }

    public void acknowledge() {
        this.acknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.active = false;
        this.cancelledAt = LocalDateTime.now();
    }
}
