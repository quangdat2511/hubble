package com.example.hubble.utils;

import android.content.Context;
import android.text.TextUtils;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ThemeSyncManager {

    public interface CompletionCallback {
        void onComplete();
    }

    private ThemeSyncManager() {
    }

    public static void syncThemeIfAuthenticated(Context context, CompletionCallback callback) {
        Context appContext = context.getApplicationContext();
        TokenManager tokenManager = new TokenManager(appContext);
        String token = tokenManager.getAccessToken();

        if (TextUtils.isEmpty(token)) {
            notifyCompleted(callback);
            return;
        }

        RetrofitClient.getApiService(appContext)
                .getTheme("Bearer " + token)
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<String>> call,
                                           Response<ApiResponse<String>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && !TextUtils.isEmpty(response.body().getResult())) {
                            ThemeManager.saveTheme(appContext, response.body().getResult());
                        }
                        notifyCompleted(callback);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                        notifyCompleted(callback);
                    }
                });
    }

    private static void notifyCompleted(CompletionCallback callback) {
        if (callback != null) {
            callback.onComplete();
        }
    }
}
