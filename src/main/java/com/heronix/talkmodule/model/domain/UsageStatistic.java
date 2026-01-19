package com.heronix.talkmodule.model.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks daily usage statistics for analytics.
 */
@Entity
@Table(name = "usage_statistics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"statisticDate", "metricType"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate statisticDate;

    @Column(nullable = false)
    private String metricType;  // e.g., "messages_sent", "active_users", "new_channels"

    private Long metricValue;

    private String metricDetails;  // JSON for additional details

    // Breakdown by category
    private Long publicChannelMessages;
    private Long privateChannelMessages;
    private Long directMessages;

    private Long activeUsers;
    private Long peakConcurrentUsers;

    private Long filesShared;
    private Long totalFileSize;

    private Long alertsSent;
    private Long newsItemsPublished;

    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        if (statisticDate == null) {
            statisticDate = LocalDate.now();
        }
    }
}
