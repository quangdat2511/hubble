package com.example.hubble.data.model.settings;

public class PushConfigResponse {
    private boolean notificationEnabled;
    private boolean notificationSound;

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public boolean isNotificationSound() {
        return notificationSound;
    }
}
