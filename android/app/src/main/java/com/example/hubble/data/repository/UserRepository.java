package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.R;
import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.me.UpdateProfileRequest;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Gson gson;
    private final Context appContext;

    public UserRepository(Context context) {
        appContext = context.getApplicationContext();
        apiService = RetrofitClient.getApiService(appContext);
        tokenManager = new TokenManager(appContext);
        gson = new Gson();
    }

    public UserResponse getCachedUser() {
        return tokenManager.getUser();
    }

    public void getProfile(RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());

        String token = getBearerToken();
        if (token == null) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }

        apiService.getProfile(token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserResponse>> call,
                                   @NonNull Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    UserResponse user = response.body().getResult();
                    tokenManager.saveUser(user);
                    callback.onResult(AuthResult.success(user));
                    return;
                }

                callback.onResult(AuthResult.error(extractErrorMessage(response, appContext.getString(R.string.profile_load_error))));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public void updateProfile(UpdateProfileRequest request, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());

        String token = getBearerToken();
        if (token == null) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }

        apiService.updateProfile(token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserResponse>> call,
                                   @NonNull Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    UserResponse user = response.body().getResult();
                    tokenManager.saveUser(user);
                    callback.onResult(AuthResult.success(user));
                    return;
                }

                callback.onResult(AuthResult.error(extractErrorMessage(response, appContext.getString(R.string.profile_update_error))));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    private String getBearerToken() {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return null;
        }
        return "Bearer " + accessToken;
    }

    private String buildNetworkError(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = appContext.getString(R.string.error_network_unknown);
        }
        return appContext.getString(R.string.error_network, message);
    }

    private String extractErrorMessage(Response<?> response, String fallback) {
        if (response == null) {
            return fallback;
        }

        Object body = response.body();
        if (body instanceof ApiResponse<?>) {
            ApiResponse<?> apiResponse = (ApiResponse<?>) body;
            String message = apiResponse.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
        }

        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().trim().isEmpty()) {
                        return apiError.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to generic message below.
        }

        return fallback + " (HTTP " + response.code() + ")";
    }
}
