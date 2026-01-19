package com.heronix.talkmodule.service;

import com.heronix.talkmodule.model.domain.CurrentSession;
import com.heronix.talkmodule.model.dto.AuthResponseDTO;
import com.heronix.talkmodule.model.dto.UserDTO;
import com.heronix.talkmodule.model.enums.ConnectionMode;
import com.heronix.talkmodule.repository.CurrentSessionRepository;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Manages the current user session and connection state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManager {

    private final CurrentSessionRepository sessionRepository;

    @Getter
    private CurrentSession currentSession;

    @Getter
    private final ObjectProperty<ConnectionMode> connectionMode = new SimpleObjectProperty<>(ConnectionMode.OFFLINE);

    @Transactional
    public void createSession(AuthResponseDTO authResponse, String serverUrl, boolean rememberMe) {
        // Clear any existing session
        sessionRepository.clearAllSessions();

        UserDTO user = authResponse.getUser();

        currentSession = CurrentSession.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .role(user.getRole())
                .sessionToken(authResponse.getSessionToken())
                .serverUrl(serverUrl)
                .connectionMode(ConnectionMode.CONNECTED)
                .lastConnected(LocalDateTime.now())
                .sessionExpires(authResponse.getExpiresAt())
                .rememberMe(rememberMe)
                .build();

        sessionRepository.save(currentSession);
        connectionMode.set(ConnectionMode.CONNECTED);

        log.info("Session created for user: {}", user.getUsername());
    }

    @Transactional
    public void createOfflineSession(String username, String password) {
        // Clear any existing session
        sessionRepository.clearAllSessions();

        currentSession = CurrentSession.builder()
                .username(username)
                .connectionMode(ConnectionMode.OFFLINE)
                .build();

        sessionRepository.save(currentSession);
        connectionMode.set(ConnectionMode.OFFLINE);

        log.info("Offline session created for user: {}", username);
    }

    @Transactional
    public Optional<CurrentSession> loadExistingSession() {
        Optional<CurrentSession> sessionOpt = sessionRepository.findLatestSession();

        if (sessionOpt.isPresent()) {
            currentSession = sessionOpt.get();

            // Check if session is still valid
            if (currentSession.hasValidSession()) {
                connectionMode.set(currentSession.getConnectionMode());
                log.info("Loaded existing session for user: {}", currentSession.getUsername());
                return sessionOpt;
            } else if (currentSession.isRememberMe()) {
                // Session expired but remember me - keep offline access
                currentSession.setConnectionMode(ConnectionMode.OFFLINE);
                connectionMode.set(ConnectionMode.OFFLINE);
                sessionRepository.save(currentSession);
                return sessionOpt;
            }
        }

        return Optional.empty();
    }

    @Transactional
    public void updateConnectionMode(ConnectionMode mode) {
        if (currentSession != null) {
            currentSession.setConnectionMode(mode);
            if (mode == ConnectionMode.CONNECTED) {
                currentSession.setLastConnected(LocalDateTime.now());
            }
            sessionRepository.save(currentSession);
        }
        connectionMode.set(mode);
    }

    @Transactional
    public void clearSession() {
        sessionRepository.clearAllSessions();
        currentSession = null;
        connectionMode.set(ConnectionMode.OFFLINE);
        log.info("Session cleared");
    }

    public boolean isLoggedIn() {
        return currentSession != null;
    }

    public boolean isConnected() {
        return connectionMode.get() == ConnectionMode.CONNECTED;
    }

    public boolean isAdmin() {
        return currentSession != null && currentSession.isAdmin();
    }

    public Long getCurrentUserId() {
        return currentSession != null ? currentSession.getUserId() : null;
    }

    public String getCurrentUsername() {
        return currentSession != null ? currentSession.getUsername() : null;
    }

    public String getSessionToken() {
        return currentSession != null ? currentSession.getSessionToken() : null;
    }

    public String getServerUrl() {
        return currentSession != null ? currentSession.getServerUrl() : null;
    }
}
