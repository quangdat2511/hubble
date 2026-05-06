package com.example.hubble.security;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSwitcherProtectionRepository {

    private static final String PREFS_NAME = "HubbleSecurityPrefs";
    private static final String KEY_APP_SWITCHER_PROTECTION = "app_switcher_protection";

    private final SharedPreferences preferences;

    public AppSwitcherProtectionRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_APP_SWITCHER_PROTECTION, false);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_APP_SWITCHER_PROTECTION, enabled)
                .apply();
    }
}
