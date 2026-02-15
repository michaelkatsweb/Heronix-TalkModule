package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local cached news item entity for offline operations.
 */
@Entity
@Table(name = "local_news_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalNewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long localId;

    @Column(unique = true)
    private Long serverId;

    private String headline;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String category;

    private Long authorId;
    private String authorName;

    @Builder.Default
    private int priority = 0;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean pinned = false;

    @Builder.Default
    private boolean urgent = false;

    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;

    // For scheduled posts
    private LocalDateTime scheduledAt;

    @Builder.Default
    private boolean published = true;

    private String linkUrl;
    private String imagePath;

    private int viewCount;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;

    private LocalDateTime lastSyncTime;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        modifiedDate = LocalDateTime.now();
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isScheduled() {
        return scheduledAt != null && LocalDateTime.now().isBefore(scheduledAt);
    }

    public boolean isVisible() {
        return active && published && !isExpired() && !isScheduled();
    }
}
