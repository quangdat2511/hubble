package com.example.hubble.security;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLockRepository {

    public static final int PIN_LENGTH = 4;
    public static final long LOCK_TIMEOUT_MS = 30_000L;

    private static final String PREFS_NAME = "HubbleAppLockPrefs";
    private static final String KEY_PASSCODE_ENABLED = "passcode_enabled";
    private static final String KEY_LAST_BACKGROUNDED_AT = "last_backgrounded_at";

    private final SharedPreferences preferences;
    private final SecurePinStorage securePinStorage;

    public AppLockRepository(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        securePinStorage = new SecurePinStorage(appContext);
    }

    public boolean hasStoredPin() {
        return securePinStorage.hasStoredPin();
    }

    public boolean isPasscodeEnabled() {
        return preferences.getBoolean(KEY_PASSCODE_ENABLED, false) && hasStoredPin();
    }

    public boolean savePin(String pin) {
        return securePinStorage.savePin(pin);
    }

    public boolean updatePin(String pin) {
        return securePinStorage.savePin(pin);
    }

    public boolean verifyPin(String pin) {
        return securePinStorage.verifyPin(pin);
    }

    public void setPasscodeEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_PASSCODE_ENABLED, enabled && hasStoredPin())
                .apply();
    }

    public void clearPin() {
        securePinStorage.clearPin();
        preferences.edit()
                .putBoolean(KEY_PASSCODE_ENABLED, false)
                .remove(KEY_LAST_BACKGROUNDED_AT)
                .apply();
    }

    public void markBackgrounded() {
        if (!isPasscodeEnabled()) {
            return;
        }
        preferences.edit()
                .putLong(KEY_LAST_BACKGROUNDED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean shouldRequireUnlock() {
        if (!isPasscodeEnabled()) {
            clearBackgroundState();
            return false;
        }

        long lastBackgroundedAt = preferences.getLong(KEY_LAST_BACKGROUNDED_AT, 0L);
        if (lastBackgroundedAt <= 0L) {
            return false;
        }

        long timeInBackground = System.currentTimeMillis() - lastBackgroundedAt;
        if (timeInBackground < LOCK_TIMEOUT_MS) {
            clearBackgroundState();
            return false;
        }
        return true;
    }

    public void clearBackgroundState() {
        preferences.edit().remove(KEY_LAST_BACKGROUNDED_AT).apply();
    }
}
