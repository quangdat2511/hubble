package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.MessageDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsRepository {

    private static final String TAG = "SettingsRepository";

    private final ApiService apiService;

    public SettingsRepository(Context context) {
        apiService = RetrofitClient.getApiService(context);
    }

    public LiveData<String> getTheme(String authHeader) {
        MutableLiveData<String> data = new MutableLiveData<>();

        Log.d(TAG, "getTheme() called");
        Log.d(TAG, "Authorization = " + authHeader);

        apiService.getTheme(authHeader).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call,
                                   Response<ApiResponse<MessageDto>> response) {

                Log.d(TAG, "getTheme onResponse: code = " + response.code());
                Log.d(TAG, "getTheme onResponse: successful = " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d(TAG, "getTheme body = " + response.body().toString());
                } else {
                    Log.d(TAG, "getTheme body = null");
                }

                if (response.isSuccessful() && response.body() != null) {
                    String theme = response.body().getMessage();
                    Log.d(TAG, "Parsed theme = " + theme);
                    data.setValue(theme);
                } else {
                    Log.e(TAG, "getTheme failed, fallback to light");
                    data.setValue("light");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                Log.e(TAG, "getTheme onFailure: " + t.getMessage(), t);
                data.setValue("light");
            }
        });

        return data;
    }

    public void updateTheme(String authHeader, String theme) {
        Log.d(TAG, "updateTheme() called");
        Log.d(TAG, "Authorization = " + authHeader);
        Log.d(TAG, "theme = " + theme);

        apiService.updateTheme(authHeader, theme).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call,
                                   Response<ApiResponse<MessageDto>> response) {

                Log.d(TAG, "updateTheme onResponse: code = " + response.code());
                Log.d(TAG, "updateTheme onResponse: successful = " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d(TAG, "updateTheme body = " + response.body().toString());
                } else {
                    Log.d(TAG, "updateTheme body = null");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                Log.e(TAG, "updateTheme onFailure: " + t.getMessage(), t);
            }
        });
    }
}