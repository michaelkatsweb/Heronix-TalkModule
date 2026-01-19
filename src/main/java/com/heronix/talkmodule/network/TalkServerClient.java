package com.heronix.talkmodule.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heronix.talkmodule.model.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * HTTP client for communicating with Heronix-Talk server.
 */
@Component
@Slf4j
public class TalkServerClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${heronix.server.url:http://localhost:9680}")
    private String serverUrl;

    @Value("${heronix.server.timeout-seconds:10}")
    private int timeoutSeconds;

    private String sessionToken;

    public TalkServerClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    public void setSessionToken(String token) {
        this.sessionToken = token;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    // ===================== Health Check =====================

    public boolean isServerReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/system/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Server not reachable: {}", e.getMessage());
            return false;
        }
    }

    // ===================== Authentication =====================

    public Optional<AuthResponseDTO> login(String username, String password, String clientType) {
        try {
            AuthRequestDTO request = AuthRequestDTO.builder()
                    .username(username)
                    .password(password)
                    .clientType(clientType)
                    .clientVersion("1.0.0")
                    .build();

            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AuthResponseDTO authResponse = objectMapper.readValue(response.body(), AuthResponseDTO.class);
                if (authResponse.isSuccess()) {
                    this.sessionToken = authResponse.getSessionToken();
                }
                return Optional.of(authResponse);
            }

            return Optional.of(AuthResponseDTO.builder()
                    .success(false)
                    .message("Authentication failed")
                    .build());

        } catch (Exception e) {
            log.error("Login error", e);
            return Optional.empty();
        }
    }

    public void logout() {
        if (sessionToken == null) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/auth/logout"))
                    .header("X-Session-Token", sessionToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.debug("Logout error", e);
        } finally {
            sessionToken = null;
        }
    }

    // ===================== Users =====================

    public List<UserDTO> getUsers() {
        return get("/api/users", new TypeReference<List<UserDTO>>() {}).orElse(List.of());
    }

    public List<UserDTO> getOnlineUsers() {
        return get("/api/users/online", new TypeReference<List<UserDTO>>() {}).orElse(List.of());
    }

    public Optional<UserDTO> getUser(Long id) {
        return get("/api/users/" + id, new TypeReference<UserDTO>() {});
    }

    public List<UserDTO> searchUsers(String query) {
        return get("/api/users/search?q=" + encode(query), new TypeReference<List<UserDTO>>() {}).orElse(List.of());
    }

    // ===================== Channels =====================

    public List<ChannelDTO> getChannels() {
        return get("/api/channels", new TypeReference<List<ChannelDTO>>() {}).orElse(List.of());
    }

    public List<ChannelDTO> getMyChannels() {
        return get("/api/channels/my", new TypeReference<List<ChannelDTO>>() {}).orElse(List.of());
    }

    public List<ChannelDTO> getPublicChannels() {
        return get("/api/channels/public", new TypeReference<List<ChannelDTO>>() {}).orElse(List.of());
    }

    public Optional<ChannelDTO> createChannel(CreateChannelRequestDTO request) {
        return post("/api/channels", request, new TypeReference<ChannelDTO>() {});
    }

    public boolean joinChannel(Long channelId) {
        return post("/api/channels/" + channelId + "/join", null, new TypeReference<Void>() {}).isPresent()
                || isSuccessfulEmptyResponse();
    }

    public boolean leaveChannel(Long channelId) {
        return post("/api/channels/" + channelId + "/leave", null, new TypeReference<Void>() {}).isPresent()
                || isSuccessfulEmptyResponse();
    }

    // ===================== Messages =====================

    public List<MessageDTO> getChannelMessages(Long channelId, int page, int size) {
        return get("/api/messages/channel/" + channelId + "?page=" + page + "&size=" + size,
                new TypeReference<List<MessageDTO>>() {}).orElse(List.of());
    }

    public Optional<MessageDTO> sendMessage(SendMessageRequestDTO request) {
        return post("/api/messages", request, new TypeReference<MessageDTO>() {});
    }

    public Optional<MessageDTO> editMessage(Long messageId, String content) {
        return put("/api/messages/" + messageId, content, new TypeReference<MessageDTO>() {});
    }

    public boolean deleteMessage(Long messageId) {
        return delete("/api/messages/" + messageId);
    }

    // ===================== News =====================

    public List<NewsItemDTO> getNews() {
        return get("/api/news", new TypeReference<List<NewsItemDTO>>() {}).orElse(List.of());
    }

    public List<NewsItemDTO> getUrgentNews() {
        return get("/api/news/urgent", new TypeReference<List<NewsItemDTO>>() {}).orElse(List.of());
    }

    public Optional<NewsItemDTO> createNews(NewsItemDTO news) {
        return post("/api/news", news, new TypeReference<NewsItemDTO>() {});
    }

    public Optional<NewsItemDTO> createUrgentNews(String headline, String content) {
        return post("/api/news/urgent",
                java.util.Map.of("headline", headline, "content", content),
                new TypeReference<NewsItemDTO>() {});
    }

    // ===================== Presence =====================

    public boolean sendHeartbeat() {
        try {
            return post("/api/presence/heartbeat", null, new TypeReference<Void>() {}).isPresent()
                    || isSuccessfulEmptyResponse();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateStatus(String status, String statusMessage) {
        return post("/api/presence/status",
                java.util.Map.of("status", status, "statusMessage", statusMessage != null ? statusMessage : ""),
                new TypeReference<Object>() {}).isPresent();
    }

    // ===================== Helper Methods =====================

    private <T> Optional<T> get(String path, TypeReference<T> typeRef) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET();

            if (sessionToken != null) {
                requestBuilder.header("X-Session-Token", sessionToken);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Optional.of(objectMapper.readValue(response.body(), typeRef));
            }
        } catch (Exception e) {
            log.error("GET {} error: {}", path, e.getMessage());
        }
        return Optional.empty();
    }

    private <T, R> Optional<R> post(String path, T body, TypeReference<R> typeRef) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json");

            if (sessionToken != null) {
                requestBuilder.header("X-Session-Token", sessionToken);
            }

            String json = body != null ? objectMapper.writeValueAsString(body) : "";
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(json));

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            lastResponseCode = response.statusCode();

            if (response.statusCode() == 200) {
                if (response.body() != null && !response.body().isEmpty()) {
                    return Optional.of(objectMapper.readValue(response.body(), typeRef));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("POST {} error: {}", path, e.getMessage());
        }
        return Optional.empty();
    }

    private <T, R> Optional<R> put(String path, T body, TypeReference<R> typeRef) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json");

            if (sessionToken != null) {
                requestBuilder.header("X-Session-Token", sessionToken);
            }

            String json = body != null ? objectMapper.writeValueAsString(body) : "";
            requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(json));

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                return Optional.of(objectMapper.readValue(response.body(), typeRef));
            }
        } catch (Exception e) {
            log.error("PUT {} error: {}", path, e.getMessage());
        }
        return Optional.empty();
    }

    private boolean delete(String path) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .DELETE();

            if (sessionToken != null) {
                requestBuilder.header("X-Session-Token", sessionToken);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.error("DELETE {} error: {}", path, e.getMessage());
            return false;
        }
    }

    private int lastResponseCode = 0;

    private boolean isSuccessfulEmptyResponse() {
        return lastResponseCode == 200;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
