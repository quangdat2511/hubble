package com.example.hubble.data.model;

public class EmailVerifyOtpRequest {
    private String email;
    private String otpCode;

    public EmailVerifyOtpRequest(String email, String otpCode) {
        this.email = email;
        this.otpCode = otpCode;
    }

    public String getEmail() { return email; }
    public String getOtpCode() { return otpCode; }
}