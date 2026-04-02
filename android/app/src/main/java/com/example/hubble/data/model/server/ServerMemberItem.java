package com.example.hubble.data.model.server;

import java.util.List;

public class ServerMemberItem {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private int avatarBackgroundColor;
    private List<ServerRoleItem> roles;
    private String status;
    private boolean isOwner;

    public ServerMemberItem(String userId, String username, String displayName, String avatarUrl,
                           int avatarBackgroundColor, List<ServerRoleItem> roles, String status, boolean isOwner) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.avatarBackgroundColor = avatarBackgroundColor;
        this.roles = roles;
        this.status = status;
        this.isOwner = isOwner;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getAvatarBackgroundColor() {
        return avatarBackgroundColor;
    }

    public List<ServerRoleItem> getRoles() {
        return roles;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public String getDisplayInitials() {
        String name = displayName != null ? displayName : username;
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    public boolean isOnline() {
        return "ONLINE".equalsIgnoreCase(status);
    }
}
