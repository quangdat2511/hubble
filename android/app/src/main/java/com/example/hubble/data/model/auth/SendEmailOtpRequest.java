package com.example.hubble.data.model.auth;

import com.google.gson.annotations.SerializedName;

public class SendEmailOtpRequest {
    @SerializedName("email")
    private String email;

    public SendEmailOtpRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
