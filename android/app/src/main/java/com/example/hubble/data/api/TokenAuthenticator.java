package com.example.hubble.data.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.RefreshTokenRequest;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.security.AppLockManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;
    private final Context context;
    public TokenAuthenticator(TokenManager tokenManager, Context context) {
        this.tokenManager = tokenManager;
        this.context = context.getApplicationContext();
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.request().url().encodedPath().contains("/api/auth/refresh")) {
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null) {
            return null;
        }
        retrofit2.Response<ApiResponse<TokenResponse>> refreshResponse = RetrofitClient.getApiService(context)
                .refreshToken(new RefreshTokenRequest(refreshToken))
                .execute();

        if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().getResult() != null) {
            TokenResponse newToken = refreshResponse.body().getResult();
            tokenManager.saveTokens(newToken.getAccessToken(), newToken.getRefreshToken());
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newToken.getAccessToken())
                    .build();
        } else {
            tokenManager.clear();
            AppLockManager manager = AppLockManager.getInstance();
            if (manager != null) {
                manager.onSessionEnded();
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            });

            return null;
        }
    }
}
