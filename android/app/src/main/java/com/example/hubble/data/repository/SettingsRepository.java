package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.viewmodel.SettingsViewModel;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private static final String DEFAULT_LANGUAGE = "vi";

    private final ApiService apiService;

    public SettingsRepository(Context context) {
        apiService = RetrofitClient.getApiService(context);
    }

    public LiveData<String> getLanguage(String authHeader) {
        MutableLiveData<String> data = new MutableLiveData<>();

        apiService.getLanguage(authHeader).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String language = response.body().getResult();
                    data.setValue(language != null && !language.trim().isEmpty()
                            ? language.trim().toLowerCase(Locale.ROOT)
                            : DEFAULT_LANGUAGE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "getLanguage failed", t);
            }
        });

        return data;
    }

    public void updateLanguage(String authHeader, String locale,
                               SettingsViewModel.SettingsUpdateCallback callback) {
        apiService.updateLanguage(authHeader, locale).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "updateLanguage success: " + response.code());
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }

                String message = "Save failed: " + response.code();
                Log.e(TAG, message);
                if (callback != null) {
                    callback.onError(message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "updateLanguage failed", t);
                if (callback != null) {
                    callback.onError("Network error");
                }
            }
        });
    }

    public LiveData<AuthResult<String>> getTheme(String authHeader) {
        MutableLiveData<AuthResult<String>> data = new MutableLiveData<>();
        data.setValue(AuthResult.loading());

        Log.d(TAG, "getTheme() called");
        Log.d(TAG, "Authorization = " + authHeader);

        apiService.getTheme(authHeader).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                Log.d(TAG, "getTheme onResponse: code = " + response.code());
                Log.d(TAG, "getTheme onResponse: successful = " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d(TAG, "getTheme body = " + response.body().toString());
                } else {
                    Log.d(TAG, "getTheme body = null");
                }

                if (response.isSuccessful() && response.body() != null) {
                    String theme = response.body().getResult();
                    Log.d(TAG, "Parsed theme = " + theme);
                    data.setValue(AuthResult.success(theme));
                } else {
                    String errorMessage = extractErrorMessage(response, "Không tải được giao diện");
                    Log.e(TAG, "getTheme failed: " + errorMessage);
                    data.setValue(AuthResult.error(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "getTheme onFailure: " + t.getMessage(), t);
                data.setValue(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });

        return data;
    }

    public LiveData<AuthResult<String>> updateTheme(String authHeader, String theme) {
        MutableLiveData<AuthResult<String>> data = new MutableLiveData<>();
        data.setValue(AuthResult.loading());

        Log.d(TAG, "updateTheme() called");
        Log.d(TAG, "Authorization = " + authHeader);
        Log.d(TAG, "theme = " + theme);

        apiService.updateTheme(authHeader, theme).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                Log.d(TAG, "updateTheme onResponse: code = " + response.code());
                Log.d(TAG, "updateTheme onResponse: successful = " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d(TAG, "updateTheme body = " + response.body().toString());
                } else {
                    Log.d(TAG, "updateTheme body = null");
                }

                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(AuthResult.success(response.body().getResult()));
                } else {
                    String errorMessage = extractErrorMessage(response, "Không cập nhật được giao diện");
                    Log.e(TAG, "updateTheme failed: " + errorMessage);
                    data.setValue(AuthResult.error(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "updateTheme onFailure: " + t.getMessage(), t);
                data.setValue(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });

        return data;
    }

    private String extractErrorMessage(Response<ApiResponse<String>> response, String fallbackMessage) {
        ApiResponse<String> body = response.body();
        if (body != null && body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
            return body.getMessage();
        }
        if (response.message() != null && !response.message().trim().isEmpty()) {
            return response.message();
        }
        return fallbackMessage;
    }
}
