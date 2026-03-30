package com.example.hubble.data.model.dm;

public class FriendRequestResponse {
    private String id;
    private String requesterId;
    private String addresseeId;
    private String status;
    private String createdAt;
    private boolean incoming;
    private FriendUserDto user;

    public String getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public String getAddresseeId() { return addresseeId; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public boolean isIncoming() { return incoming; }
    public FriendUserDto getUser() { return user; }
}