package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.viewmodel.SettingsViewModel;

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
                            ? language.trim().toLowerCase()
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
}
