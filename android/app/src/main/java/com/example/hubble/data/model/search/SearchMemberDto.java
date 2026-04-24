package com.example.hubble.data.model.search;

import com.google.gson.annotations.SerializedName;

public class SearchMemberDto {
    private String id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String status;
    @SerializedName(value = "isSelf", alternate = {"self"})
    private boolean isSelf;
    @SerializedName(value = "isFriend", alternate = {"friend"})
    private boolean isFriend;
    @SerializedName(value = "friendshipState", alternate = {"friendState"})
    private String friendshipState;

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public boolean isSelf() { return isSelf; }
    public boolean isFriend() { return isFriend; }
    public String getFriendshipState() { return friendshipState; }
}
