package com.example.hubble.data.model.server;

/**
 * Maps to backend MemberBriefResponse DTO.
 */
public class MemberBriefResponse {
    private String serverMemberId;
    private String userId;
    private String displayName;
    private String username;
    private String avatarUrl;

    public String getServerMemberId() { return serverMemberId; }
    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
}
