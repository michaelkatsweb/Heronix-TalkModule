package com.heronix.talkmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessageDTO {
    private String type;
    private String action;
    private Object payload;
    private Long userId;
    private Long channelId;
    private String correlationId;
    private boolean success;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Type constants
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_CHANNEL = "CHANNEL";
    public static final String TYPE_USER = "USER";
    public static final String TYPE_PRESENCE = "PRESENCE";
    public static final String TYPE_TYPING = "TYPING";
    public static final String TYPE_NOTIFICATION = "NOTIFICATION";
    public static final String TYPE_NEWS = "NEWS";
    public static final String TYPE_ALERT = "ALERT";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_ACK = "ACK";
}
