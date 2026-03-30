package com.example.hubble.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hubble.data.model.auth.UserResponse;
import com.google.gson.Gson;

public class TokenManager {
    private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String KEY_REFRESH_TOKEN = "REFRESH_TOKEN";
    private static final String KEY_USER_INFO = "USER_INFO";
    private static final String KEY_LAST_BASE_URL = "LAST_BASE_URL";

    private final SharedPreferences prefs;
    private final Gson gson;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences("HubblePrefs", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public void saveUser(UserResponse user) {
        prefs.edit().putString(KEY_USER_INFO, gson.toJson(user)).apply();
    }

    public UserResponse getUser() {
        String userStr = prefs.getString(KEY_USER_INFO, null);
        if (userStr != null) {
            return gson.fromJson(userStr, UserResponse.class);
        }
        return null;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean clearSessionIfBaseUrlChanged(String currentBaseUrl) {
        if (currentBaseUrl == null || currentBaseUrl.trim().isEmpty()) {
            return false;
        }

        String normalizedCurrent = currentBaseUrl.trim();
        String previous = prefs.getString(KEY_LAST_BASE_URL, null);
        if (previous == null || previous.trim().isEmpty()) {
            prefs.edit().putString(KEY_LAST_BASE_URL, normalizedCurrent).apply();
            return false;
        }

        if (previous.trim().equals(normalizedCurrent)) {
            return false;
        }

        prefs.edit().clear().apply();
        prefs.edit().putString(KEY_LAST_BASE_URL, normalizedCurrent).apply();
        return true;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}