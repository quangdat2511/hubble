package com.example.hubble.data.model.auth;

public class SessionDto {
    private String id;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private String lastActiveAt;
    private String createdAt;

    public String getId() { return id; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public String getIpAddress() { return ipAddress; }
    public String getLastActiveAt() { return lastActiveAt; }
    public String getCreatedAt() { return createdAt; }
}