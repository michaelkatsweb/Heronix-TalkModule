package com.heronix.talkmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.heronix.talkmodule.model.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDTO {
    private Long id;
    private String messageUuid;
    private Long channelId;
    private String channelName;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageType messageType;
    private String status;
    private boolean edited;
    private boolean deleted;
    private boolean pinned;
    private boolean important;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime editedAt;

    private Long replyToId;
    private String replyToPreview;
    private String replyToSenderName;
    private int replyCount;

    private String attachmentPath;
    private String attachmentName;
    private String attachmentType;
    private Long attachmentSize;

    private String reactions;
    private String mentions;
    private String clientId;

    public boolean hasAttachment() {
        return attachmentPath != null && !attachmentPath.isEmpty();
    }

    public boolean isReply() {
        return replyToId != null;
    }
}
