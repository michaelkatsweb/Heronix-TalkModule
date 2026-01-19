package com.heronix.talkmodule.model.dto;

import com.heronix.talkmodule.model.enums.ChannelType;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequestDTO {
    private String name;
    private String description;
    private ChannelType channelType;
    private String icon;
    private List<Long> memberIds;
    private boolean notifyMembers;
}
