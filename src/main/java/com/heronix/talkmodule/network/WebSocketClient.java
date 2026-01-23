package com.heronix.talkmodule.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heronix.talkmodule.model.dto.WebSocketMessageDTO;
import javafx.application.Platform;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time messaging with Heronix-Talk server.
 */
public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<Consumer<WebSocketMessageDTO>> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private volatile boolean reconnecting = false;
    private volatile boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int BASE_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 60000;
    private static final int HEARTBEAT_INTERVAL_MS = 30000;

    // Scheduled executor for reconnection and heartbeat
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "talkmodule-ws-scheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> heartbeatTask;
    private volatile long lastPongTime = System.currentTimeMillis();

    // Queue for pending messages during reconnection
    private final BlockingQueue<WebSocketMessageDTO> pendingMessages = new LinkedBlockingQueue<>(100);

    public WebSocketClient(URI serverUri, ObjectMapper objectMapper) {
        super(serverUri);
        this.objectMapper = objectMapper;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("WebSocket connection established");
        reconnecting = false;
        reconnectAttempts = 0;
        notifyConnectionListeners(true);
        startHeartbeat();
        flushPendingMessages();
    }

    @Override
    public void onMessage(String message) {
        try {
            WebSocketMessageDTO wsMessage = objectMapper.readValue(message, WebSocketMessageDTO.class);
            log.debug("Received WebSocket message: type={}, action={}", wsMessage.getType(), wsMessage.getAction());

            // Notify on JavaFX thread
            Platform.runLater(() -> {
                for (Consumer<WebSocketMessageDTO> listener : messageListeners) {
                    try {
                        listener.accept(wsMessage);
                    } catch (Exception e) {
                        log.error("Error in message listener", e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Error parsing WebSocket message: {}", message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket connection closed: code={}, reason={}, remote={}", code, reason, remote);
        stopHeartbeat();
        notifyConnectionListeners(false);

        // Attempt reconnection if it was unexpected and we should reconnect
        if (remote && shouldReconnect && !reconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
    }

    private void scheduleReconnect() {
        if (!shouldReconnect || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log.warn("Max reconnect attempts reached ({}) or reconnect disabled", reconnectAttempts);
            return;
        }

        reconnecting = true;
        reconnectAttempts++;

        // Exponential backoff with jitter
        int delayMs = Math.min(BASE_RECONNECT_DELAY_MS * (1 << reconnectAttempts), MAX_RECONNECT_DELAY_MS);
        delayMs += (int) (Math.random() * 1000); // Add jitter

        log.info("Scheduling reconnect attempt {} of {} in {}ms", reconnectAttempts, MAX_RECONNECT_ATTEMPTS, delayMs);

        scheduler.schedule(() -> {
            if (reconnecting && shouldReconnect) {
                try {
                    reconnect();
                } catch (Exception e) {
                    log.error("Reconnection failed", e);
                    scheduleReconnect();
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Start heartbeat to detect stale connections
     */
    private void startHeartbeat() {
        stopHeartbeat();
        lastPongTime = System.currentTimeMillis();

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                // Check if connection is stale
                long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
                if (timeSinceLastPong > HEARTBEAT_INTERVAL_MS * 2) {
                    log.warn("Connection appears stale (no pong in {}ms), forcing reconnect", timeSinceLastPong);
                    try {
                        closeConnection(1000, "Stale connection");
                    } catch (Exception e) {
                        log.debug("Error closing stale connection: {}", e.getMessage());
                    }
                    return;
                }

                // Send ping
                sendPing();
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    @Override
    public void onWebsocketPong(org.java_websocket.WebSocket conn, org.java_websocket.framing.Framedata f) {
        lastPongTime = System.currentTimeMillis();
    }

    /**
     * Flush pending messages after reconnection
     */
    private void flushPendingMessages() {
        if (pendingMessages.isEmpty()) return;

        log.info("Flushing {} pending messages", pendingMessages.size());
        scheduler.execute(() -> {
            WebSocketMessageDTO msg;
            while ((msg = pendingMessages.poll()) != null && isOpen()) {
                sendMessage(msg);
            }
        });
    }

    public void addMessageListener(Consumer<WebSocketMessageDTO> listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(Consumer<WebSocketMessageDTO> listener) {
        messageListeners.remove(listener);
    }

    public void addConnectionListener(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(Consumer<Boolean> listener) {
        connectionListeners.remove(listener);
    }

    private void notifyConnectionListeners(boolean connected) {
        Platform.runLater(() -> {
            for (Consumer<Boolean> listener : connectionListeners) {
                try {
                    listener.accept(connected);
                } catch (Exception e) {
                    log.error("Error in connection listener", e);
                }
            }
        });
    }

    public void sendMessage(WebSocketMessageDTO message) {
        if (!isOpen()) {
            // Queue message for later delivery
            if ("MESSAGE".equals(message.getType())) {
                if (pendingMessages.offer(message)) {
                    log.debug("Message queued for later delivery (queue size: {})", pendingMessages.size());
                } else {
                    log.warn("Pending message queue full, message dropped");
                }
            }
            log.warn("Cannot send message: WebSocket not connected");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            send(json);
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
            // Queue for retry if it's a chat message
            if ("MESSAGE".equals(message.getType())) {
                pendingMessages.offer(message);
            }
        }
    }

    public void sendTypingIndicator(Long channelId, boolean isTyping) {
        WebSocketMessageDTO message = WebSocketMessageDTO.builder()
                .type("TYPING")
                .action(isTyping ? "TYPING_START" : "TYPING_STOP")
                .channelId(channelId)
                .payload(java.util.Map.of("channelId", channelId, "isTyping", isTyping))
                .build();
        sendMessage(message);
    }

    public void sendChatMessage(Long channelId, String content, String clientId) {
        WebSocketMessageDTO message = WebSocketMessageDTO.builder()
                .type("MESSAGE")
                .action("CREATE")
                .channelId(channelId)
                .payload(java.util.Map.of(
                        "channelId", channelId,
                        "content", content,
                        "clientId", clientId
                ))
                .build();
        sendMessage(message);
    }

    public void joinChannel(Long channelId) {
        WebSocketMessageDTO message = WebSocketMessageDTO.builder()
                .type("CHANNEL")
                .action("JOIN")
                .channelId(channelId)
                .build();
        sendMessage(message);
    }

    public void markAsRead(Long channelId, Long messageId) {
        WebSocketMessageDTO message = WebSocketMessageDTO.builder()
                .type("CHANNEL")
                .action("READ")
                .channelId(channelId)
                .payload(java.util.Map.of("channelId", channelId, "messageId", messageId))
                .build();
        sendMessage(message);
    }

    public void cancelReconnect() {
        reconnecting = false;
        shouldReconnect = false;
    }

    /**
     * Gracefully shutdown the WebSocket client
     */
    public void shutdown() {
        log.info("Shutting down WebSocket client...");
        shouldReconnect = false;
        reconnecting = false;
        stopHeartbeat();
        pendingMessages.clear();

        if (isOpen()) {
            try {
                closeBlocking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("WebSocket client shutdown complete");
    }

    /**
     * Get connection statistics for monitoring
     */
    public ConnectionStats getConnectionStats() {
        return new ConnectionStats(isOpen(), reconnectAttempts, pendingMessages.size(), lastPongTime);
    }

    public record ConnectionStats(boolean connected, int reconnectAttempts, int pendingMessageCount, long lastPongTime) {}
}
