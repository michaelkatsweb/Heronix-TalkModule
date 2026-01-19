package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.MessageType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Local cached message entity for offline operations.
 */
@Entity
@Table(name = "local_messages", indexes = {
        @Index(name = "idx_local_msg_channel", columnList = "channelId"),
        @Index(name = "idx_local_msg_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long localId;  // Local auto-generated ID

    private Long serverId;  // Server-assigned ID (null if not yet synced)

    @Column(unique = true, nullable = false)
    private String messageUuid;

    private Long channelId;
    private String channelName;

    private Long senderId;
    private String senderName;
    private String senderAvatar;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private LocalDateTime timestamp;
    private LocalDateTime editedAt;

    @Builder.Default
    private boolean edited = false;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private boolean pinned = false;

    @Builder.Default
    private boolean important = false;

    // Reply information
    private Long replyToId;
    private String replyToPreview;
    private String replyToSenderName;
    private int replyCount;

    // Attachment information
    private String attachmentPath;
    private String attachmentName;
    private String attachmentType;
    private Long attachmentSize;

    // Reactions and mentions
    @Lob
    private String reactions;

    private String mentions;

    // Client tracking
    private String clientId;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;

    private LocalDateTime lastSyncTime;

    @PrePersist
    protected void onCreate() {
        if (messageUuid == null) {
            messageUuid = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public boolean needsSync() {
        return syncStatus == SyncStatus.PENDING || syncStatus == SyncStatus.LOCAL_ONLY;
    }

    public boolean hasAttachment() {
        return attachmentPath != null && !attachmentPath.isEmpty();
    }

    public boolean isReply() {
        return replyToId != null;
    }

    public String getPreview() {
        if (deleted) return "[Deleted]";
        if (content == null || content.isEmpty()) {
            if (hasAttachment()) return "[Attachment: " + attachmentName + "]";
            return "[Empty message]";
        }
        return content.length() > 100 ? content.substring(0, 97) + "..." : content;
    }
}
