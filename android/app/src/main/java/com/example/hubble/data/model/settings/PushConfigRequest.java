package com.example.hubble.data.model.settings;

public class PushConfigRequest {
    private final boolean notificationEnabled;
    private final boolean notificationSound;

    public PushConfigRequest(boolean notificationEnabled, boolean notificationSound) {
        this.notificationEnabled = notificationEnabled;
        this.notificationSound = notificationSound;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public boolean isNotificationSound() {
        return notificationSound;
    }
}
