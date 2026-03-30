package com.example.hubble.data.model.me;

public class UpdateProfileRequest {
    private String bio;
    private String displayName;
    private String status;
    private String phone;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String bio, String displayName, String status, String phone) {
        this.bio = bio;
        this.displayName = displayName;
        this.status = status;
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}