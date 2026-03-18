package com.example.hubble.data.model.auth;

public class PhoneSendOtpRequest {
    private String phone;

    public PhoneSendOtpRequest(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}

