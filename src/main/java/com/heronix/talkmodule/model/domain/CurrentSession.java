package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.ConnectionMode;
import com.heronix.talkmodule.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores current user session information locally.
 */
@Entity
@Table(name = "current_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String department;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String sessionToken;
    private String serverUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ConnectionMode connectionMode = ConnectionMode.OFFLINE;

    private LocalDateTime lastConnected;
    private LocalDateTime sessionExpires;

    @Builder.Default
    private boolean rememberMe = false;

    // Preferences
    @Builder.Default
    private boolean notificationsEnabled = true;

    @Builder.Default
    private boolean soundEnabled = true;

    @Builder.Default
    private String theme = "dark";

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.PRINCIPAL;
    }

    public boolean isConnected() {
        return connectionMode == ConnectionMode.CONNECTED;
    }

    public boolean hasValidSession() {
        return sessionToken != null && !sessionToken.isEmpty()
                && (sessionExpires == null || LocalDateTime.now().isBefore(sessionExpires));
    }
}
