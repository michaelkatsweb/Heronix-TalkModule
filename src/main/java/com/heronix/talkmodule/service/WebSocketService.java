package com.heronix.talkmodule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heronix.talkmodule.model.dto.EmergencyAlertDTO;
import com.heronix.talkmodule.model.dto.MessageDTO;
import com.heronix.talkmodule.model.dto.NewsItemDTO;
import com.heronix.talkmodule.model.dto.WebSocketMessageDTO;
import com.heronix.talkmodule.model.enums.ConnectionMode;
import com.heronix.talkmodule.network.WebSocketClient;
import jakarta.annotation.PreDestroy;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for managing WebSocket connections to Heronix-Talk server.
 * Routes incoming messages to appropriate services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final AlertService alertService;
    private final NewsManagementService newsService;

    private WebSocketClient webSocketClient;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean connected = false;

    // External callbacks
    private Consumer<Boolean> onConnectionStateChange;
    private Consumer<String> onError;

    /**
     * Connect to the WebSocket server
     */
    public CompletableFuture<Boolean> connect(String serverUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build WebSocket URL
                String wsUrl = serverUrl.replace("http://", "ws://")
                        .replace("https://", "wss://")
                        + "/ws/chat?token=" + sessionManager.getSessionToken();

                log.info("Connecting to WebSocket: {}", wsUrl);

                webSocketClient = new WebSocketClient(new URI(wsUrl), objectMapper);
                setupMessageHandlers();

                // Connect with timeout
                boolean success = webSocketClient.connectBlocking(10, TimeUnit.SECONDS);

                if (success) {
                    connected = true;
                    startHeartbeat();
                    notifyConnectionState(true);
                    log.info("WebSocket connected successfully");
                } else {
                    log.warn("WebSocket connection failed");
                    notifyConnectionState(false);
                }

                return success;

            } catch (Exception e) {
                log.error("Error connecting to WebSocket", e);
                notifyConnectionState(false);
                return false;
            }
        });
    }

    /**
     * Disconnect from WebSocket
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.cancelReconnect();
            webSocketClient.close();
            webSocketClient = null;
        }
        connected = false;
        notifyConnectionState(false);
    }

    /**
     * Setup message handlers to route incoming WebSocket messages
     */
    private void setupMessageHandlers() {
        webSocketClient.addMessageListener(this::handleMessage);

        webSocketClient.addConnectionListener(isConnected -> {
            this.connected = isConnected;
            sessionManager.updateConnectionMode(isConnected ? ConnectionMode.CONNECTED : ConnectionMode.DISCONNECTED);
            notifyConnectionState(isConnected);
        });
    }

    /**
     * Handle incoming WebSocket message and route to appropriate service
     */
    private void handleMessage(WebSocketMessageDTO wsMessage) {
        log.debug("Received WebSocket message: type={}, action={}", wsMessage.getType(), wsMessage.getAction());

        try {
            switch (wsMessage.getType()) {
                case "MESSAGE" -> handleMessageEvent(wsMessage);
                case "TYPING" -> handleTypingEvent(wsMessage);
                case "PRESENCE" -> handlePresenceEvent(wsMessage);
                case "CHANNEL" -> handleChannelEvent(wsMessage);
                case "NEWS" -> handleNewsEvent(wsMessage);
                case "ALERT" -> handleAlertEvent(wsMessage);
                case "NOTIFICATION" -> handleNotificationEvent(wsMessage);
                case "ERROR" -> handleErrorEvent(wsMessage);
                default -> log.debug("Unhandled message type: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    private void handleMessageEvent(WebSocketMessageDTO wsMessage) {
        if (wsMessage.getPayload() == null) return;

        try {
            String action = wsMessage.getAction();

            // Handle HISTORY action - payload is a List
            if ("HISTORY".equals(action)) {
                String payloadJson = objectMapper.writeValueAsString(wsMessage.getPayload());
                java.util.List<MessageDTO> messages = objectMapper.readValue(payloadJson,
                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, MessageDTO.class));
                log.info("Received {} history messages for channel {}", messages.size(), wsMessage.getChannelId());
                // History is loaded via REST, this is a secondary path
                return;
            }

            // Handle CREATE/UPDATE/DELETE - single message
            String payloadJson = objectMapper.writeValueAsString(wsMessage.getPayload());
            MessageDTO message = objectMapper.readValue(payloadJson, MessageDTO.class);
            log.info("Received message: id={}, channelId={}", message.getId(), message.getChannelId());

            // Route to ChatService
            chatService.receiveMessage(message);

        } catch (Exception e) {
            log.error("Error parsing message payload", e);
        }
    }

    private void handleTypingEvent(WebSocketMessageDTO wsMessage) {
        // Typing indicators - could be displayed in UI
        log.debug("Typing event: action={}, channelId={}", wsMessage.getAction(), wsMessage.getChannelId());
    }

    private void handlePresenceEvent(WebSocketMessageDTO wsMessage) {
        // User presence updates
        log.debug("Presence event: action={}", wsMessage.getAction());
    }

    private void handleChannelEvent(WebSocketMessageDTO wsMessage) {
        // Channel events (join, leave, update)
        String action = wsMessage.getAction();
        log.debug("Channel event: action={}", action);

        if ("UPDATE".equals(action) || "JOIN".equals(action)) {
            // Refresh channels list
            chatService.loadChannels();
        }
    }

    private void handleNewsEvent(WebSocketMessageDTO wsMessage) {
        if (wsMessage.getPayload() == null) return;

        try {
            String payloadJson = objectMapper.writeValueAsString(wsMessage.getPayload());
            NewsItemDTO newsItem = objectMapper.readValue(payloadJson, NewsItemDTO.class);
            log.info("News received: {}", newsItem.getHeadline());

            // Route to NewsService
            newsService.receiveNewsItem(newsItem);

        } catch (Exception e) {
            log.error("Error parsing news payload", e);
        }
    }

    private void handleAlertEvent(WebSocketMessageDTO wsMessage) {
        if (wsMessage.getPayload() == null) return;

        try {
            // Parse the alert DTO from payload
            String payloadJson = objectMapper.writeValueAsString(wsMessage.getPayload());
            EmergencyAlertDTO alertDto = objectMapper.readValue(payloadJson, EmergencyAlertDTO.class);

            log.warn("ALERT received via WebSocket: [{}] {} - {}",
                    alertDto.getAlertLevel(), alertDto.getAlertType(), alertDto.getTitle());

            // Route to AlertService - this handles saving, displaying, and playing sounds
            alertService.receiveAlert(alertDto);

        } catch (Exception e) {
            log.error("Error parsing alert payload", e);
            // Fallback: just reload active alerts
            alertService.loadActiveAlerts();
        }
    }

    private void handleNotificationEvent(WebSocketMessageDTO wsMessage) {
        log.debug("Notification event: {}", wsMessage.getAction());
    }

    private void handleErrorEvent(WebSocketMessageDTO wsMessage) {
        String error = wsMessage.getPayload() != null ? wsMessage.getPayload().toString() : "Unknown error";
        log.error("WebSocket error from server: {}", error);

        if (onError != null) {
            Platform.runLater(() -> onError.accept(error));
        }
    }

    /**
     * Start heartbeat to keep connection alive
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (connected && webSocketClient != null) {
                try {
                    WebSocketMessageDTO heartbeat = WebSocketMessageDTO.builder()
                            .type("PING")
                            .action("HEARTBEAT")
                            .build();
                    webSocketClient.sendMessage(heartbeat);
                } catch (Exception e) {
                    log.debug("Heartbeat failed: {}", e.getMessage());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Send a chat message via WebSocket
     */
    public void sendMessage(Long channelId, String content, String clientId) {
        if (webSocketClient != null && connected) {
            webSocketClient.sendChatMessage(channelId, content, clientId);
        }
    }

    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(Long channelId, boolean isTyping) {
        if (webSocketClient != null && connected) {
            webSocketClient.sendTypingIndicator(channelId, isTyping);
        }
    }

    /**
     * Join a channel via WebSocket
     */
    public void joinChannel(Long channelId) {
        if (webSocketClient != null && connected) {
            webSocketClient.joinChannel(channelId);
        }
    }

    /**
     * Mark messages as read
     */
    public void markAsRead(Long channelId, Long messageId) {
        if (webSocketClient != null && connected) {
            webSocketClient.markAsRead(channelId, messageId);
        }
    }

    private void notifyConnectionState(boolean isConnected) {
        if (onConnectionStateChange != null) {
            Platform.runLater(() -> onConnectionStateChange.accept(isConnected));
        }
    }

    public void setOnConnectionStateChange(Consumer<Boolean> handler) {
        this.onConnectionStateChange = handler;
    }

    public void setOnError(Consumer<String> handler) {
        this.onError = handler;
    }

    public boolean isConnected() {
        return connected;
    }

    @PreDestroy
    public void shutdown() {
        disconnect();
        heartbeatScheduler.shutdown();
    }
}
