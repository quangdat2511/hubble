package com.example.hubble.data.model.dm;

public class ChannelDto {
    private String id;
    private String serverId;
    private String parentId;
    private String name;
    private String type;
    private String topic;
    private Short position;
    private Boolean isPrivate;
    private String createdAt;
    private String peerUserId;
    private String peerUsername;
    private String peerDisplayName;
    private String peerAvatarUrl;
    private String peerStatus;
    private Integer unreadCount;

    public String getId() {
        return id;
    }

    public String getServerId() {
        return serverId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTopic() {
        return topic;
    }

    public Short getPosition() {
        return position;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPeerUserId() {
        return peerUserId;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    public String getPeerDisplayName() {
        return peerDisplayName;
    }

    public String getPeerAvatarUrl() {
        return peerAvatarUrl;
    }

    public String getPeerStatus() {
        return peerStatus;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
}

