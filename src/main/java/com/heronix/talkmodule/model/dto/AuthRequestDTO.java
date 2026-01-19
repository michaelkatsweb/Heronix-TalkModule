package com.heronix.talkmodule.model.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    private String username;
    private String password;
    private String clientType;
    private String clientVersion;
    private String deviceName;
    private boolean rememberMe;
}
