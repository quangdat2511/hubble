package com.example.hubble.data.model;

public class UserModel {
    private String uid;
    private String username;
    private String email;
    private String phone;
    private long createdAt;

    public UserModel() {}

    public UserModel(String uid, String username, String email, String phone) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
