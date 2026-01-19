package com.heronix.talkmodule.model.dto;

import com.heronix.talkmodule.model.enums.MessageType;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDTO {
    private Long channelId;
    private String content;
    private MessageType messageType;
    private Long replyToId;
    private String clientId;
    private List<Long> mentionedUserIds;
    private String attachmentName;
    private String attachmentType;
    private Long attachmentSize;
}
