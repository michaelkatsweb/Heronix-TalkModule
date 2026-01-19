package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.ChannelType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local cached channel entity for offline operations.
 */
@Entity
@Table(name = "local_channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Auto-generated for local, can be overwritten when synced from server

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChannelType channelType = ChannelType.PUBLIC;

    private String icon;

    private Long creatorId;
    private String creatorName;

    private int memberCount;
    private int messageCount;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean archived = false;

    @Builder.Default
    private boolean pinned = false;

    private String directMessageKey;

    private LocalDateTime lastMessageTime;

    // User-specific settings
    @Builder.Default
    private boolean muted = false;

    @Builder.Default
    private boolean favorite = false;

    @Builder.Default
    private int unreadCount = 0;

    private Long lastReadMessageId;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    private LocalDateTime lastSyncTime;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        modifiedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    public boolean isDirectMessage() {
        return channelType == ChannelType.DIRECT_MESSAGE;
    }

    public boolean isPublic() {
        return channelType == ChannelType.PUBLIC;
    }
}
