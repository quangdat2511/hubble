package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.settings.PushConfigRequest;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PushConfigRepository {

    private final TokenManager tokenManager;
    private final ApiService apiService;

    public PushConfigRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
        this.apiService = RetrofitClient.getApiService(appContext);
    }

    public void getPushConfig(RepositoryCallback<PushConfigResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getPushConfig(token).enqueue(new Callback<ApiResponse<PushConfigResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PushConfigResponse>> call,
                                   @NonNull Response<ApiResponse<PushConfigResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }

                String error = response.body() != null ? response.body().getMessage() : null;
                callback.onResult(AuthResult.error(error != null ? error : "Khong tai duoc cai dat thong bao"));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PushConfigResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error("Loi ket noi: " + t.getMessage()));
            }
        });
    }

    public void updatePushConfig(boolean notificationEnabled,
                                 boolean notificationSound,
                                 RepositoryCallback<PushConfigResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        PushConfigRequest request = new PushConfigRequest(notificationEnabled, notificationSound);
        apiService.updatePushConfig(token, request).enqueue(new Callback<ApiResponse<PushConfigResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PushConfigResponse>> call,
                                   @NonNull Response<ApiResponse<PushConfigResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }

                String error = response.body() != null ? response.body().getMessage() : null;
                callback.onResult(AuthResult.error(error != null ? error : "Khong luu duoc cai dat thong bao"));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PushConfigResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error("Loi ket noi: " + t.getMessage()));
            }
        });
    }

    private <T> String requireAuthToken(RepositoryCallback<T> callback) {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Ban chua dang nhap"));
            return null;
        }
        return "Bearer " + accessToken;
    }
}
