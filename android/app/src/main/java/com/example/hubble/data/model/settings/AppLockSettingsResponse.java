package com.example.hubble.data.model.settings;

public class AppLockSettingsResponse {
    private String appLockPin;
    private boolean pinConfigured;

    public String getAppLockPin() {
        return appLockPin;
    }

    public boolean isPinConfigured() {
        return pinConfigured;
    }

    public void setAppLockPin(String appLockPin) {
        this.appLockPin = appLockPin;
    }

    public void setPinConfigured(boolean pinConfigured) {
        this.pinConfigured = pinConfigured;
    }
}
