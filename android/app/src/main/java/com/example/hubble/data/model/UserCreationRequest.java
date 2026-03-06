package com.example.hubble.data.model;

public class UserCreationRequest {
    private String firebaseUid;
    private String username;
    private String displayName;
    private String email;
    private String phone;

    public UserCreationRequest(String firebaseUid, String username, String displayName, String email, String phone) {
        this.firebaseUid = firebaseUid;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
    }
}