package com.heronix.talkmodule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST client for Heronix SIS Server API.
 * Used by TalkModule to search students and load parent contacts.
 */
@Slf4j
@Service
public class SisApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${heronix.sis.url:http://localhost:9580}")
    private String sisBaseUrl;

    public SisApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Search students by name or ID.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchStudents(String searchTerm) {
        try {
            String url = sisBaseUrl + "/api/students/search?q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
            log.warn("Student search returned status {}", response.statusCode());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to search students: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get parent/guardian contacts for a student.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getParentContacts(Long studentId) {
        try {
            String url = sisBaseUrl + "/api/students/" + studentId + "/parents";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
            log.warn("Parent contacts returned status {}", response.statusCode());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to load parent contacts: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get message history for a parent.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMessageHistory(Long parentId) {
        try {
            String url = sisBaseUrl + "/api/parent-communications/history?parentId=" + parentId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to load message history: {}", e.getMessage());
            return List.of();
        }
    }
}
