package com.example.hubble.utils;

import android.content.Context;
import android.text.TextUtils;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.settings.AppLockSettingsResponse;
import com.example.hubble.security.AppLockRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AppLockSyncManager {

    public interface CompletionCallback {
        void onComplete();
    }

    private AppLockSyncManager() {
    }

    public static void syncAppLockIfAuthenticated(Context context, CompletionCallback callback) {
        Context appContext = context.getApplicationContext();
        TokenManager tokenManager = new TokenManager(appContext);
        String token = tokenManager.getAccessToken();

        if (TextUtils.isEmpty(token) || tokenManager.getUser() == null) {
            notifyCompleted(callback);
            return;
        }

        AppLockRepository repository = new AppLockRepository(appContext);
        RetrofitClient.getApiService(appContext)
                .getAppLockSettings("Bearer " + token)
                .enqueue(new Callback<ApiResponse<AppLockSettingsResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AppLockSettingsResponse>> call,
                                           Response<ApiResponse<AppLockSettingsResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {
                            AppLockSettingsResponse settings = response.body().getResult();
                            repository.syncPinFromServer(settings.getAppLockPin());
                        }
                        notifyCompleted(callback);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AppLockSettingsResponse>> call, Throwable t) {
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
