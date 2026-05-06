package com.example.hubble.data.model.settings;

public class AppLockSettingsRequest {
    private final String appLockPin;

    public AppLockSettingsRequest(String appLockPin) {
        this.appLockPin = appLockPin;
    }

    public String getAppLockPin() {
        return appLockPin;
    }
}
