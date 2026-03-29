package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.SessionDto;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionRepository {
    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Gson gson;

    public SessionRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(context.getApplicationContext());
        this.gson = new Gson();
    }

    public void getActiveSessions(RepositoryCallback<List<SessionDto>> callback) {
        callback.onResult(AuthResult.loading());
        String token = "Bearer " + tokenManager.getAccessToken();

        apiService.getActiveSessions(token).enqueue(new Callback<ApiResponse<List<SessionDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<SessionDto>>> call, @NonNull Response<ApiResponse<List<SessionDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    // Trích xuất lỗi thực tế từ Backend
                    callback.onResult(AuthResult.error(extractErrorMessage(response, "Không thể lấy danh sách phiên")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<SessionDto>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    public void revokeSession(String sessionId, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        String token = "Bearer " + tokenManager.getAccessToken();

        apiService.revokeSession(token, sessionId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(extractErrorMessage(response, "Không thể đăng xuất thiết bị")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    // Hàm tiện ích giúp parse JSON Error từ Spring Boot
    private <T> String extractErrorMessage(Response<T> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String errorRaw = response.errorBody().string();
                if (!errorRaw.isEmpty()) {
                    ApiResponse<?> apiError = gson.fromJson(errorRaw, ApiResponse.class);
                    if (apiError != null && apiError.getMessage() != null) {
                        return apiError.getMessage() + " (HTTP " + response.code() + ")";
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SessionRepo", "Lỗi parse error body", e);
        }
        return fallback + " (HTTP " + response.code() + ")";
    }
}