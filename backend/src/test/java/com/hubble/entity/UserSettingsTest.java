package com.hubble.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserSettingsTest {

    @Test
    void normalizeTheme_StoresUppercaseTrimmedValue() {
        UserSettings settings = UserSettings.builder()
                .theme(" LIGHT ")
                .build();

        settings.normalizeTheme();

        assertEquals("LIGHT", settings.getTheme());
    }

    @Test
    void normalizeTheme_NullTheme_RemainsNull() {
        UserSettings settings = UserSettings.builder().build();

        settings.normalizeTheme();

        assertNull(settings.getTheme());
    }
}
