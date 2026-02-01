package com.heronix.talkmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Parent Portal messaging
 *
 * Used by TalkModule to send messages to parents through
 * the Heronix-Talk Parent Portal messaging service.
 *
 * Note: All data is tokenized before being sent to the
 * external Parent Portal application.
 *
 * @author Heronix TalkModule Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentMessageDTO {

    private Long studentId;
    private String category;
    private String priority;
    private String subject;
    private String content;
    private String parentToken;
    private boolean requiresAcknowledgment;
    private DeliveryOptions deliveryOptions;
    private Map<String, Object> metadata;

    /**
     * Message categories
     */
    public enum Category {
        NOTIFICATION,
        ANNOUNCEMENT,
        DIRECT_MESSAGE,
        ALERT,
        HALL_PASS,
        ATTENDANCE,
        GRADES,
        BEHAVIOR,
        HEALTH,
        CALENDAR,
        EMERGENCY
    }

    /**
     * Message priorities
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT,
        EMERGENCY
    }

    /**
     * Delivery channel options
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryOptions {
        @Builder.Default
        private boolean inApp = true;
        private boolean pushNotification;
        private boolean email;
        private boolean sms;
    }

    /**
     * Response from message service
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean success;
        private String message;
        private String messageRef;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }

    /**
     * Service status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private boolean available;
        private boolean parentPortalEnabled;
        private String serviceVersion;
        private String message;
        private List<String> supportedChannels;
        private List<String> supportedCategories;
    }

    /**
     * Simple notification
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
        private Long studentId;
        private String category;
        private String title;
        private String message;
        private String parentToken;
    }

    /**
     * Alert message
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private Long studentId;
        private String alertType;
        private String title;
        private String message;
        private String parentToken;
        private boolean requiresImmediateAction;
        private String actionUrl;
    }

    /**
     * Hall pass notification
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HallPassNotification {
        private Long studentId;
        private String passType;
        private String destination;
        private LocalDateTime departureTime;
        private LocalDateTime returnTime;
        private Integer durationMinutes;
        private String parentToken;
    }

    /**
     * School announcement
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Announcement {
        private String subject;
        private String content;
        private String category;
        private List<String> parentTokens;
        private LocalDateTime effectiveDate;
        private LocalDateTime expirationDate;
    }

    /**
     * Bulk send response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkResponse {
        private boolean success;
        private String message;
        private int totalRecipients;
        private int successCount;
        private int failureCount;
        private List<String> messageRefs;
    }
}
