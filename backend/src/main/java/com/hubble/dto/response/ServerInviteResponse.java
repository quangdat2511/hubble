package com.hubble.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInviteResponse {
    private String id;
    private String serverId;
    private String serverName;
    private String serverIconUrl;
    private String inviterId;
    private String inviterUsername;
    private String inviterDisplayName;
    private String inviteeId;
    private String inviteeUsername;
    private String inviteeDisplayName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
}

