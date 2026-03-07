package com.example.hubble.data.model;

public class PhoneVerifyOtpRequest {
    private String phone;
    private String otpCode;

    public PhoneVerifyOtpRequest(String phone, String otpCode) {
        this.phone = phone;
        this.otpCode = otpCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getOtpCode() {
        return otpCode;
    }
}