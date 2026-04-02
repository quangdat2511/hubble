package com.hubble.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerMemberResponse {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String status;
    private boolean isOwner;
    private List<RoleDto> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDto {
        private String id;
        private String name;
        private String color;
    }
}
