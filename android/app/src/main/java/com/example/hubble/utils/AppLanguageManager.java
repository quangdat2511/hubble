package com.example.hubble.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public final class AppLanguageManager {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_VIETNAMESE = "vi";
    public static final String DEFAULT_LANGUAGE = LANGUAGE_VIETNAMESE;

    private AppLanguageManager() {
    }

    public static void applyAppLanguage(String languageCode) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(normalize(languageCode))
        );
    }

    public static String getCurrentLanguage(Context context) {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty() && appLocales.get(0) != null) {
            return normalize(appLocales.get(0).getLanguage());
        }

        Locale locale = ConfigurationCompat.getLocales(
                context.getResources().getConfiguration()
        ).get(0);

        return locale != null ? normalize(locale.getLanguage()) : DEFAULT_LANGUAGE;
    }

    public static String normalize(String languageCode) {
        if (languageCode == null) {
            return DEFAULT_LANGUAGE;
        }

        String normalized = languageCode.trim().toLowerCase(Locale.ROOT);
        return LANGUAGE_ENGLISH.equals(normalized) ? LANGUAGE_ENGLISH : LANGUAGE_VIETNAMESE;
    }
}
