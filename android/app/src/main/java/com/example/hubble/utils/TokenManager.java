package com.example.hubble.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hubble.data.model.UserResponse;
import com.google.gson.Gson;

public class TokenManager {
    private final SharedPreferences prefs;
    private final Gson gson;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences("HubblePrefs", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString("ACCESS_TOKEN", accessToken)
                .putString("REFRESH_TOKEN", refreshToken)
                .apply();
    }

    public void saveUser(UserResponse user) {
        prefs.edit().putString("USER_INFO", gson.toJson(user)).apply();
    }

    public UserResponse getUser() {
        String userStr = prefs.getString("USER_INFO", null);
        if (userStr != null) {
            return gson.fromJson(userStr, UserResponse.class);
        }
        return null;
    }

    public String getAccessToken() {
        return prefs.getString("ACCESS_TOKEN", null);
    }

    public String getRefreshToken() {
        return prefs.getString("REFRESH_TOKEN", null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}