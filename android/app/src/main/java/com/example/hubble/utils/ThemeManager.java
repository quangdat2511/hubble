package com.example.hubble.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";

    private static final String PREFS_NAME = "HubblePrefs";
    private static final String KEY_THEME = "APP_THEME";

    private ThemeManager() {
    }

    public static void applyStoredTheme(Context context) {
        applyTheme(getSavedTheme(context));
    }

    public static void saveTheme(Context context, String theme) {
        String normalizedTheme = normalizeTheme(theme);
        getPreferences(context)
                .edit()
                .putString(KEY_THEME, normalizedTheme)
                .apply();
        applyTheme(normalizedTheme);
    }

    public static String getSavedTheme(Context context) {
        return normalizeTheme(getPreferences(context).getString(KEY_THEME, THEME_DARK));
    }

    public static String normalizeTheme(String theme) {
        return THEME_LIGHT.equalsIgnoreCase(theme) ? THEME_LIGHT : THEME_DARK;
    }

    public static void applyTheme(String theme) {
        AppCompatDelegate.setDefaultNightMode(
                THEME_DARK.equals(normalizeTheme(theme))
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
