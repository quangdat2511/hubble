package com.example.hubble.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    public static final String THEME_SYSTEM = "SYSTEM";
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
        return normalizeTheme(getPreferences(context).getString(KEY_THEME, THEME_SYSTEM));
    }

    public static String normalizeTheme(String theme) {
        if (THEME_LIGHT.equalsIgnoreCase(theme)) {
            return THEME_LIGHT;
        }
        if (THEME_DARK.equalsIgnoreCase(theme)) {
            return THEME_DARK;
        }
        return THEME_SYSTEM;
    }

    public static void applyTheme(String theme) {
        switch (normalizeTheme(theme)) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static String resolveEffectiveTheme(Context context, String theme) {
        String normalizedTheme = normalizeTheme(theme);
        if (!THEME_SYSTEM.equals(normalizedTheme)) {
            return normalizedTheme;
        }

        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES ? THEME_DARK : THEME_LIGHT;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
