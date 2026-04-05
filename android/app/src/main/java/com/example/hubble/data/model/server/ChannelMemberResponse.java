package com.example.hubble.data.model.server;

public class ChannelMemberResponse {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isOwner;

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isOwner() { return isOwner; }
}
