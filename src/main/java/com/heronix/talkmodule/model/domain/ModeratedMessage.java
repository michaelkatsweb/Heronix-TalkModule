package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks moderated/flagged messages for admin review.
 */
@Entity
@Table(name = "moderated_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModeratedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageId;
    private String messageUuid;

    private Long channelId;
    private String channelName;

    private Long senderId;
    private String senderName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String originalContent;

    private LocalDateTime messageTimestamp;

    // Moderation details
    private Long moderatorId;
    private String moderatorName;

    private LocalDateTime moderatedAt;

    private String moderationReason;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private boolean warned = false;

    @Builder.Default
    private boolean reviewed = false;

    private String moderatorNotes;

    // Flag source
    @Builder.Default
    private boolean autoFlagged = false;  // System flagged

    @Builder.Default
    private boolean userReported = false;  // User reported

    private Long reportedById;
    private String reportedByName;
    private String reportReason;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;

    private LocalDateTime lastSyncTime;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    public boolean isPending() {
        return !reviewed;
    }
}
