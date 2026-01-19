package com.heronix.talkmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.heronix.talkmodule.model.enums.ChannelType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelDTO {
    private Long id;
    private String name;
    private String description;
    private ChannelType channelType;
    private String icon;
    private Long creatorId;
    private String creatorName;
    private int memberCount;
    private int messageCount;
    private boolean active;
    private boolean archived;
    private boolean pinned;
    private String directMessageKey;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    // User-specific
    private boolean muted;
    private boolean favorite;
    private int unreadCount;
    private Long lastReadMessageId;

    public boolean isDirectMessage() {
        return channelType == ChannelType.DIRECT_MESSAGE;
    }

    public boolean isPublic() {
        return channelType == ChannelType.PUBLIC;
    }
}
