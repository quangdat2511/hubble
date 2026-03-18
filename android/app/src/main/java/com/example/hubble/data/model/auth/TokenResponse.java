package com.example.hubble.data.model.auth;

public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserResponse user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public long getExpiresIn() { return expiresIn; }
    public UserResponse getUser() { return user; }

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public void setUser(UserResponse user) { this.user = user; }
}

