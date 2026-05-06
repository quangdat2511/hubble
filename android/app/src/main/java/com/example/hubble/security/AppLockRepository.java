package com.example.hubble.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.utils.TokenManager;

public class AppLockRepository {

    public static final int PIN_LENGTH = 4;

    private static final String PREFS_NAME = "HubbleAppLockPrefs";
    private static final String KEY_PASSCODE_ENABLED_PREFIX = "passcode_enabled_";

    private final SharedPreferences preferences;
    private final SecurePinStorage securePinStorage;
    private final TokenManager tokenManager;

    public AppLockRepository(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        securePinStorage = new SecurePinStorage(appContext);
        tokenManager = new TokenManager(appContext);
    }

    public boolean hasStoredPin() {
        String userId = getCurrentUserId();
        return userId != null && securePinStorage.hasStoredPin(userId);
    }

    public boolean isPasscodeEnabled() {
        String userId = getCurrentUserId();
        return userId != null
                && preferences.getBoolean(buildEnabledKey(userId), false)
                && hasStoredPin();
    }

    public boolean savePin(String pin) {
        String userId = getCurrentUserId();
        return userId != null && securePinStorage.savePin(userId, pin);
    }

    public boolean updatePin(String pin) {
        return savePin(pin);
    }

    public boolean verifyPin(String pin) {
        String userId = getCurrentUserId();
        return userId != null && securePinStorage.verifyPin(userId, pin);
    }

    public void setPasscodeEnabled(boolean enabled) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        preferences.edit()
                .putBoolean(buildEnabledKey(userId), enabled && hasStoredPin())
                .apply();
    }

    public void clearPin() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        securePinStorage.clearPin(userId);
        preferences.edit().putBoolean(buildEnabledKey(userId), false).apply();
    }

    public void syncPinFromServer(@Nullable String pin) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        String normalizedPin = normalizePin(pin);
        if (normalizedPin == null) {
            securePinStorage.clearPin(userId);
            preferences.edit().putBoolean(buildEnabledKey(userId), false).apply();
            return;
        }

        securePinStorage.savePin(userId, normalizedPin);
    }

    @Nullable
    public String getCurrentUserId() {
        UserResponse user = tokenManager.getUser();
        if (user == null || TextUtils.isEmpty(user.getId())) {
            return null;
        }
        return user.getId();
    }

    public void clearBackgroundState() {
        // Kept as a no-op so existing callers continue to compile after moving to immediate re-locking.
    }

    private String buildEnabledKey(String userId) {
        return KEY_PASSCODE_ENABLED_PREFIX + userId;
    }

    @Nullable
    private String normalizePin(@Nullable String pin) {
        if (pin == null) {
            return null;
        }

        String normalizedPin = pin.trim();
        if (normalizedPin.matches("\\d{" + PIN_LENGTH + "}")) {
            return normalizedPin;
        }
        return null;
    }
}
