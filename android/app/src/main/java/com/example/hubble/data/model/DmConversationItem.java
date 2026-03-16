package com.example.hubble.data.model;

public class DmConversationItem {

    private final String id;
    private final String channelId;
    private final String friendId;
    private final String displayName;
    private final String lastMessage;
    private final String timeLabel;
    private final boolean online;
    private final boolean verified;
    private final boolean selected;

    public DmConversationItem(
            String id,
            String channelId,
            String friendId,
            String displayName,
            String lastMessage,
            String timeLabel,
            boolean online,
            boolean verified,
            boolean selected
    ) {
        this.id = id;
        this.channelId = channelId;
        this.friendId = friendId;
        this.displayName = displayName;
        this.lastMessage = lastMessage;
        this.timeLabel = timeLabel;
        this.online = online;
        this.verified = verified;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getFriendId() {
        return friendId;
    }

    public boolean hasChannelId() {
        return channelId != null && !channelId.trim().isEmpty();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isSelected() {
        return selected;
    }
}
