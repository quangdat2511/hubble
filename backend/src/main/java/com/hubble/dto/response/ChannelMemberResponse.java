package com.hubble.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMemberResponse {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isOwner;
}
