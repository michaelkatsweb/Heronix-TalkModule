package com.heronix.talkmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.heronix.talkmodule.model.enums.AlertLevel;
import com.heronix.talkmodule.model.enums.AlertType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmergencyAlertDTO {
    private Long id;
    private String alertUuid;
    private String title;
    private String message;
    private String instructions;
    private AlertLevel alertLevel;
    private AlertType alertType;
    private Long issuedById;
    private String issuedByName;
    private boolean active;
    private boolean acknowledged;
    private boolean requiresAcknowledgment;
    private boolean playSound;
    private boolean campusWide;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}
