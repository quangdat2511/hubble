package com.example.hubble.data.model.settings;

public class DeviceAlertSettingsRequest {
    private final boolean enabled;

    public DeviceAlertSettingsRequest(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
