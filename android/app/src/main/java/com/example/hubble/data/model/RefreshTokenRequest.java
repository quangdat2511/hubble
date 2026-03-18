package com.example.hubble.data.model;

public class RefreshTokenRequest {
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public String getRefreshToken() { return refreshToken; }
}