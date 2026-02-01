package com.heronix.talkmodule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heronix.talkmodule.model.dto.ParentMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Parent Message Service for TalkModule
 *
 * Communicates with Heronix-Talk's Parent Portal messaging service
 * to send tokenized messages to parents.
 *
 * Features:
 * - Send direct messages to parents
 * - Send notifications and alerts
 * - Hall pass notifications
 * - School announcements
 * - Emergency broadcasts
 * - Async message delivery
 *
 * Note: All student data is tokenized by Heronix-Talk before
 * being sent to the external Parent Portal application.
 *
 * @author Heronix TalkModule Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ParentMessageService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${heronix.server.url:http://localhost:9680}")
    private String serverUrl;

    @Value("${heronix.parent-portal.enabled:true}")
    private boolean parentPortalEnabled;

    private String sessionToken;

    private static final String PARENT_PORTAL_API_PATH = "/api/parent-portal/messages";

    public ParentMessageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Set session token for authenticated requests
     */
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /**
     * Set server URL
     */
    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    // ========================================================================
    // MESSAGE SENDING
    // ========================================================================

    /**
     * Send a message to a parent
     */
    public CompletableFuture<ParentMessageDTO.Response> sendMessage(ParentMessageDTO message) {
        return CompletableFuture.supplyAsync(() -> {
            if (!parentPortalEnabled) {
                return ParentMessageDTO.Response.builder()
                        .success(false)
                        .message("Parent Portal messaging is disabled")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("studentId", message.getStudentId());
                requestBody.put("category", message.getCategory());
                requestBody.put("priority", message.getPriority());
                requestBody.put("subject", message.getSubject());
                requestBody.put("content", message.getContent());
                requestBody.put("parentToken", message.getParentToken());
                requestBody.put("requiresAcknowledgment", message.isRequiresAcknowledgment());

                if (message.getDeliveryOptions() != null) {
                    Map<String, Boolean> delivery = new HashMap<>();
                    delivery.put("inApp", message.getDeliveryOptions().isInApp());
                    delivery.put("pushNotification", message.getDeliveryOptions().isPushNotification());
                    delivery.put("email", message.getDeliveryOptions().isEmail());
                    delivery.put("sms", message.getDeliveryOptions().isSms());
                    requestBody.put("deliveryChannels", delivery);
                }

                if (message.getMetadata() != null) {
                    requestBody.put("metadata", message.getMetadata());
                }

                return executePost(PARENT_PORTAL_API_PATH + "/send", requestBody);

            } catch (Exception e) {
                log.error("Failed to send parent message", e);
                return ParentMessageDTO.Response.builder()
                        .success(false)
                        .message("Failed to send message: " + e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        });
    }

    /**
     * Send a notification to a parent
     */
    public CompletableFuture<ParentMessageDTO.Response> sendNotification(
            ParentMessageDTO.Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            if (!parentPortalEnabled) {
                return disabledResponse();
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("studentId", notification.getStudentId());
                requestBody.put("category", notification.getCategory());
                requestBody.put("title", notification.getTitle());
                requestBody.put("message", notification.getMessage());
                requestBody.put("parentToken", notification.getParentToken());

                return executePost(PARENT_PORTAL_API_PATH + "/notify", requestBody);

            } catch (Exception e) {
                log.error("Failed to send notification", e);
                return errorResponse("notification", e);
            }
        });
    }

    /**
     * Send an urgent alert to a parent
     */
    public CompletableFuture<ParentMessageDTO.Response> sendAlert(ParentMessageDTO.Alert alert) {
        return CompletableFuture.supplyAsync(() -> {
            if (!parentPortalEnabled) {
                return disabledResponse();
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("studentId", alert.getStudentId());
                requestBody.put("alertType", alert.getAlertType());
                requestBody.put("title", alert.getTitle());
                requestBody.put("message", alert.getMessage());
                requestBody.put("parentToken", alert.getParentToken());
                requestBody.put("requiresImmediateAction", alert.isRequiresImmediateAction());
                requestBody.put("actionUrl", alert.getActionUrl());

                return executePost(PARENT_PORTAL_API_PATH + "/alert", requestBody);

            } catch (Exception e) {
                log.error("Failed to send alert", e);
                return errorResponse("alert", e);
            }
        });
    }

    /**
     * Send a hall pass notification to a parent
     */
    public CompletableFuture<ParentMessageDTO.Response> sendHallPassNotification(
            ParentMessageDTO.HallPassNotification notification) {
        return CompletableFuture.supplyAsync(() -> {
            if (!parentPortalEnabled) {
                return disabledResponse();
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("studentId", notification.getStudentId());
                requestBody.put("passType", notification.getPassType());
                requestBody.put("destination", notification.getDestination());
                requestBody.put("parentToken", notification.getParentToken());

                if (notification.getDepartureTime() != null) {
                    requestBody.put("departureTime", notification.getDepartureTime().toString());
                }
                if (notification.getReturnTime() != null) {
                    requestBody.put("returnTime", notification.getReturnTime().toString());
                }
                if (notification.getDurationMinutes() != null) {
                    requestBody.put("durationMinutes", notification.getDurationMinutes());
                }

                return executePost(PARENT_PORTAL_API_PATH + "/hall-pass", requestBody);

            } catch (Exception e) {
                log.error("Failed to send hall pass notification", e);
                return errorResponse("hall pass notification", e);
            }
        });
    }

    /**
     * Send a school announcement to multiple parents
     */
    public CompletableFuture<ParentMessageDTO.BulkResponse> sendAnnouncement(
            ParentMessageDTO.Announcement announcement) {
        return CompletableFuture.supplyAsync(() -> {
            if (!parentPortalEnabled) {
                return ParentMessageDTO.BulkResponse.builder()
                        .success(false)
                        .message("Parent Portal messaging is disabled")
                        .build();
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("subject", announcement.getSubject());
                requestBody.put("content", announcement.getContent());
                requestBody.put("category", announcement.getCategory());
                requestBody.put("parentTokens", announcement.getParentTokens());

                if (announcement.getEffectiveDate() != null) {
                    requestBody.put("effectiveDate", announcement.getEffectiveDate().toString());
                }
                if (announcement.getExpirationDate() != null) {
                    requestBody.put("expirationDate", announcement.getExpirationDate().toString());
                }

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + PARENT_PORTAL_API_PATH + "/announcement"))
                        .header("Content-Type", "application/json")
                        .header("X-Session-Token", sessionToken != null ? sessionToken : "")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), ParentMessageDTO.BulkResponse.class);
                } else {
                    log.error("Announcement send failed with status: {}", response.statusCode());
                    return ParentMessageDTO.BulkResponse.builder()
                            .success(false)
                            .message("Failed with status: " + response.statusCode())
                            .build();
                }

            } catch (Exception e) {
                log.error("Failed to send announcement", e);
                return ParentMessageDTO.BulkResponse.builder()
                        .success(false)
                        .message("Failed to send announcement: " + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Send emergency broadcast to all parents
     */
    public CompletableFuture<ParentMessageDTO.BulkResponse> sendEmergencyBroadcast(
            String title, String message, List<String> parentTokens) {
        ParentMessageDTO.Announcement announcement = ParentMessageDTO.Announcement.builder()
                .subject("[EMERGENCY] " + title)
                .content(message)
                .category("EMERGENCY")
                .parentTokens(parentTokens)
                .effectiveDate(LocalDateTime.now())
                .build();

        return sendAnnouncement(announcement);
    }

    // ========================================================================
    // SERVICE STATUS
    // ========================================================================

    /**
     * Check if parent messaging service is available
     */
    public CompletableFuture<ParentMessageDTO.ServiceStatus> getServiceStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + PARENT_PORTAL_API_PATH + "/status"))
                        .header("X-Session-Token", sessionToken != null ? sessionToken : "")
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), ParentMessageDTO.ServiceStatus.class);
                } else {
                    return ParentMessageDTO.ServiceStatus.builder()
                            .available(false)
                            .message("Service returned status: " + response.statusCode())
                            .build();
                }

            } catch (Exception e) {
                log.error("Failed to check service status", e);
                return ParentMessageDTO.ServiceStatus.builder()
                        .available(false)
                        .message("Service unavailable: " + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Check if service is available (synchronous)
     */
    public boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + PARENT_PORTAL_API_PATH + "/status"))
                    .header("X-Session-Token", sessionToken != null ? sessionToken : "")
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            log.debug("Service availability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if parent portal is enabled
     */
    public boolean isParentPortalEnabled() {
        return parentPortalEnabled;
    }

    // ========================================================================
    // CONVENIENCE METHODS
    // ========================================================================

    /**
     * Quick send: Simple notification
     */
    public CompletableFuture<ParentMessageDTO.Response> quickNotify(
            Long studentId, String parentToken, String title, String message) {
        return sendNotification(ParentMessageDTO.Notification.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .category("NOTIFICATION")
                .title(title)
                .message(message)
                .build());
    }

    /**
     * Quick send: Hall pass departure
     */
    public CompletableFuture<ParentMessageDTO.Response> notifyHallPassDeparture(
            Long studentId, String parentToken, String destination) {
        return sendHallPassNotification(ParentMessageDTO.HallPassNotification.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .passType("DEPARTURE")
                .destination(destination)
                .departureTime(LocalDateTime.now())
                .build());
    }

    /**
     * Quick send: Hall pass return
     */
    public CompletableFuture<ParentMessageDTO.Response> notifyHallPassReturn(
            Long studentId, String parentToken, String destination, int durationMinutes) {
        return sendHallPassNotification(ParentMessageDTO.HallPassNotification.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .passType("RETURN")
                .destination(destination)
                .returnTime(LocalDateTime.now())
                .durationMinutes(durationMinutes)
                .build());
    }

    /**
     * Quick send: Urgent alert
     */
    public CompletableFuture<ParentMessageDTO.Response> sendUrgentAlert(
            Long studentId, String parentToken, String alertType, String title, String message) {
        return sendAlert(ParentMessageDTO.Alert.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .alertType(alertType)
                .title(title)
                .message(message)
                .requiresImmediateAction(true)
                .build());
    }

    /**
     * Quick send: Attendance notification
     */
    public CompletableFuture<ParentMessageDTO.Response> sendAttendanceNotification(
            Long studentId, String parentToken, String attendanceType, String details) {
        return sendNotification(ParentMessageDTO.Notification.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .category("ATTENDANCE")
                .title("Attendance Update: " + attendanceType)
                .message(details)
                .build());
    }

    /**
     * Quick send: Grade notification
     */
    public CompletableFuture<ParentMessageDTO.Response> sendGradeNotification(
            Long studentId, String parentToken, String subject, String gradeInfo) {
        return sendNotification(ParentMessageDTO.Notification.builder()
                .studentId(studentId)
                .parentToken(parentToken)
                .category("GRADES")
                .title("Grade Update: " + subject)
                .message(gradeInfo)
                .build());
    }

    // ========================================================================
    // INTERNAL HELPERS
    // ========================================================================

    private ParentMessageDTO.Response executePost(String path, Map<String, Object> body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + path))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", sessionToken != null ? sessionToken : "")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ParentMessageDTO.Response.class);
            } else {
                log.error("Request to {} failed with status: {}", path, response.statusCode());
                return ParentMessageDTO.Response.builder()
                        .success(false)
                        .message("Request failed with status: " + response.statusCode())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            log.error("Request to {} failed", path, e);
            return ParentMessageDTO.Response.builder()
                    .success(false)
                    .message("Request failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private ParentMessageDTO.Response disabledResponse() {
        return ParentMessageDTO.Response.builder()
                .success(false)
                .message("Parent Portal messaging is disabled")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ParentMessageDTO.Response errorResponse(String operation, Exception e) {
        return ParentMessageDTO.Response.builder()
                .success(false)
                .message("Failed to send " + operation + ": " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
