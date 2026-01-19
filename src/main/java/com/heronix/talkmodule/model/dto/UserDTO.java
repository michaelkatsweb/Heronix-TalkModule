package com.heronix.talkmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.heronix.talkmodule.model.enums.UserRole;
import com.heronix.talkmodule.model.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private Long id;
    private String username;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private String statusMessage;
    private String avatarPath;
    private boolean active;
    private boolean notificationsEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeen;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isOnline() {
        return status != null && status != UserStatus.OFFLINE;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.PRINCIPAL;
    }
}
