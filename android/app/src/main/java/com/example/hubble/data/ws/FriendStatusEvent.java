package com.example.hubble.data.ws;

public class FriendStatusEvent {
    private String userId;
    private String status;
    private String customStatus;
    private String lastSeenAt;

    public FriendStatusEvent() {}

    public FriendStatusEvent(String userId, String status, String customStatus, String lastSeenAt) {
        this.userId = userId;
        this.status = status;
        this.customStatus = customStatus;
        this.lastSeenAt = lastSeenAt;
    }

    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getCustomStatus() { return customStatus; }
    public String getLastSeenAt() { return lastSeenAt; }
}
