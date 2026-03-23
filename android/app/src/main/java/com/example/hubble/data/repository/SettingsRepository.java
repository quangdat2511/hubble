package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.viewmodel.SettingsViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private final ApiService apiService;

    public SettingsRepository(Context context) {
        apiService = RetrofitClient.getApiService(context);
    }

    public LiveData<String> getLanguage(String authHeader) {
        MutableLiveData<String> data = new MutableLiveData<>();

        apiService.getLanguage(authHeader).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call,
                                   Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Assuming the 'result' field of ApiResponse contains the language string,
                    // as 'message' is typically for status messages.
                    // But if your backend puts the locale in 'message', this stays as is.
                    String language = response.body().getMessage();
                    data.setValue(language != null ? language : "en");
                } else {
                    data.setValue("en");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                Log.e(TAG, "getLanguage failed", t);
                data.setValue("en");
            }
        });

        return data;
    }

    public void updateLanguage(String authHeader, String locale,
                               SettingsViewModel.SettingsUpdateCallback callback) {
        apiService.updateLanguage(authHeader, locale).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call,
                                   Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "updateLanguage success: " + response.code());
                    if (callback != null) callback.onSuccess();
                } else {
                    String message = "Save failed: " + response.code();
                    Log.e(TAG, message);
                    if (callback != null) callback.onError(message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                Log.e(TAG, "updateLanguage failed", t);
                if (callback != null) {
                    callback.onError("Network error");
                }
            }
        });
    }
}
