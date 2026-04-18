package com.example.hubble.data.model.search;

public class SearchMemberDto {
    private String id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String status;
    private boolean isSelf;

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public boolean isSelf() { return isSelf; }
}
