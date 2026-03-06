package com.example.hubble.data.model;

public class PhoneLoginRequest {
    private String phone;
    private String password;

    public PhoneLoginRequest(String phone, String password) {
        this.phone = phone;
        this.password = password;
    }

    public String getPhone() { return phone; }
    public String getPassword() { return password; }
}
