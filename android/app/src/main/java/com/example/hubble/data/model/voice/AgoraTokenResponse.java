package com.example.hubble.data.model.voice;

public class AgoraTokenResponse {
    private String token;
    private int uid;
    private int expiresIn;
    private String appId;

    public AgoraTokenResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
}
