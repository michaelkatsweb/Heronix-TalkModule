package com.heronix.talkmodule.model.domain;

import com.heronix.talkmodule.model.enums.SyncStatus;
import com.heronix.talkmodule.model.enums.UserRole;
import com.heronix.talkmodule.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local cached user entity for offline operations.
 */
@Entity
@Table(name = "local_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalUser {

    @Id
    private Long id;  // Server-assigned ID

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String employeeId;

    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String phoneNumber;
    private String avatarPath;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.STAFF;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.OFFLINE;

    private String statusMessage;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime lastSeen;
    private LocalDateTime lastActivity;

    // Sync tracking
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    private LocalDateTime lastSyncTime;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        modifiedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isOnline() {
        return status != UserStatus.OFFLINE;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.PRINCIPAL;
    }
}
