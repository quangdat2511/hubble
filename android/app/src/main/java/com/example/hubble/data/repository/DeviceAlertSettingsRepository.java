package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.settings.DeviceAlertSettingsRequest;
import com.example.hubble.data.model.settings.DeviceAlertSettingsResponse;
import com.example.hubble.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceAlertSettingsRepository {

    private final TokenManager tokenManager;
    private final ApiService apiService;

    public DeviceAlertSettingsRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
        this.apiService = RetrofitClient.getApiService(appContext);
    }

    public void getSettings(RepositoryCallback<DeviceAlertSettingsResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getDeviceAlertSettings(token).enqueue(new Callback<ApiResponse<DeviceAlertSettingsResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<DeviceAlertSettingsResponse>> call,
                                   @NonNull Response<ApiResponse<DeviceAlertSettingsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }

                String error = response.body() != null ? response.body().getMessage() : null;
                callback.onResult(AuthResult.error(error != null ? error : "Khong tai duoc canh bao dang nhap"));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<DeviceAlertSettingsResponse>> call,
                                  @NonNull Throwable t) {
                callback.onResult(AuthResult.error("Loi ket noi: " + t.getMessage()));
            }
        });
    }

    public void updateSettings(boolean enabled, RepositoryCallback<DeviceAlertSettingsResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        DeviceAlertSettingsRequest request = new DeviceAlertSettingsRequest(enabled);
        apiService.updateDeviceAlertSettings(token, request)
                .enqueue(new Callback<ApiResponse<DeviceAlertSettingsResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<DeviceAlertSettingsResponse>> call,
                                           @NonNull Response<ApiResponse<DeviceAlertSettingsResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                            return;
                        }

                        String error = response.body() != null ? response.body().getMessage() : null;
                        callback.onResult(AuthResult.error(error != null ? error : "Khong luu duoc canh bao dang nhap"));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<DeviceAlertSettingsResponse>> call,
                                          @NonNull Throwable t) {
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
