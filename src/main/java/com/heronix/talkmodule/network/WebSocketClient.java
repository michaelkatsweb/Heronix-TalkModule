package com.heronix.talkmodule.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heronix.talkmodule.model.dto.WebSocketMessageDTO;
import javafx.application.Platform;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time messaging with Heronix-Talk server.
 */
public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<Consumer<WebSocketMessageDTO>> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private boolean reconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 3000;

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
        notifyConnectionListeners(false);

        // Attempt reconnection if it was unexpected
        if (remote && !reconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
    }

    private void scheduleReconnect() {
        reconnecting = true;
        reconnectAttempts++;
        log.info("Scheduling reconnect attempt {} of {}", reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

        new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
                if (reconnecting) {
                    reconnect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
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
        try {
            String json = objectMapper.writeValueAsString(message);
            send(json);
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
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
    }
}
