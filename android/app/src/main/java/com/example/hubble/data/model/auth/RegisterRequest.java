package com.example.hubble.data.model.auth;

public class RegisterRequest {
    private String username;
    private String displayName;
    private String email;
    private String password;

    public RegisterRequest(String username, String displayName, String email, String password) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}

