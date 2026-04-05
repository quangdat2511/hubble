package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.notify.NotificationResponse;
import com.example.hubble.utils.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {
    private final ApiService apiService;
    private final TokenManager tokenManager;

    public NotificationRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(context.getApplicationContext());
    }

    private String getToken() {
        String token = tokenManager.getAccessToken();
        return (token != null && !token.isEmpty()) ? "Bearer " + token : null;
    }

    public void getNotifications(int page, int size, RepositoryCallback<List<NotificationResponse>> callback) {
        String token = getToken();
        if (token == null) {
            callback.onResult(AuthResult.error("Unauthorized"));
            return;
        }
        apiService.getNotifications(token, page, size).enqueue(new Callback<ApiResponse<List<NotificationResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<NotificationResponse>>> call, @NonNull Response<ApiResponse<List<NotificationResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi tải thông báo"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<NotificationResponse>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void getUnreadCount(RepositoryCallback<Long> callback) {
        String token = getToken();
        if (token == null) {
            callback.onResult(AuthResult.error("Unauthorized"));
            return;
        }
        apiService.getNotificationUnreadCount(token).enqueue(new Callback<ApiResponse<Long>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Long>> call, @NonNull Response<ApiResponse<Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi tải số thông báo"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Long>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void markAsRead(String notificationId, RepositoryCallback<Void> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.markNotificationRead(token, notificationId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success(null));
                } else {
                    callback.onResult(AuthResult.error("Lỗi đánh dấu đã đọc"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void markAllAsRead(RepositoryCallback<Void> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.markAllNotificationsRead(token).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success(null));
                } else {
                    callback.onResult(AuthResult.error("Lỗi đánh dấu đã đọc"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void registerDeviceToken(String fcmToken) {
        String token = getToken();
        if (token == null) {
            android.util.Log.w("NotificationRepo", "Access token is null, cannot register device token");
            return;
        }
        Map<String, String> body = new HashMap<>();
        body.put("token", fcmToken);
        body.put("deviceType", "ANDROID");
        apiService.registerDeviceToken(token, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.i("NotificationRepo", "Device token registered successfully");
                } else {
                    android.util.Log.e("NotificationRepo", "Failed to register device token: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                android.util.Log.e("NotificationRepo", "Error registering device token", t);
            }
        });
    }

    public void removeDeviceToken(String fcmToken) {
        String token = getToken();
        if (token == null) {
            android.util.Log.w("NotificationRepo", "Access token is null, cannot remove device token");
            return;
        }
        apiService.removeDeviceToken(token, fcmToken).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.i("NotificationRepo", "Device token removed successfully");
                } else {
                    android.util.Log.e("NotificationRepo", "Failed to remove device token: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                android.util.Log.e("NotificationRepo", "Error removing device token", t);
            }
        });
    }
}
