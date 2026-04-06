package com.example.hubble.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.R;
import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.viewmodel.SettingsViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private static final String DEFAULT_LANGUAGE = "vi";

    private final Context appContext;
    private final ApiService apiService;

    public interface LanguageFetchCallback {
        void onSuccess(String language);
        void onError(String message);
    }

    public SettingsRepository(Context context) {
        appContext = context.getApplicationContext();
        apiService = RetrofitClient.getApiService(appContext);
    }

    public LiveData<AuthResult<String>> getTheme(String authHeader) {
        MutableLiveData<AuthResult<String>> data = new MutableLiveData<>();
        data.setValue(AuthResult.loading());

        apiService.getTheme(authHeader).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(AuthResult.success(response.body().getResult()));
                } else {
                    data.setValue(AuthResult.error(extractErrorMessage(
                            response,
                            appContext.getString(R.string.error_generic)
                    )));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "getTheme failed", t);
                data.setValue(AuthResult.error(appContext.getString(R.string.error_network_unknown)));
            }
        });

        return data;
    }

    public LiveData<AuthResult<String>> updateTheme(String authHeader, String theme) {
        MutableLiveData<AuthResult<String>> data = new MutableLiveData<>();
        data.setValue(AuthResult.loading());

        apiService.updateTheme(authHeader, theme).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(AuthResult.success(response.body().getResult()));
                } else {
                    data.setValue(AuthResult.error(extractErrorMessage(
                            response,
                            appContext.getString(R.string.error_generic)
                    )));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "updateTheme failed", t);
                data.setValue(AuthResult.error(appContext.getString(R.string.error_network_unknown)));
            }
        });

        return data;
    }

    public LiveData<String> getLanguage(String authHeader) {
        MutableLiveData<String> data = new MutableLiveData<>();

        apiService.getLanguage(authHeader).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(normalizeLanguage(response.body().getResult()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "getLanguage failed", t);
            }
        });

        return data;
    }

    public void getLanguage(String authHeader, LanguageFetchCallback callback) {
        apiService.getLanguage(authHeader).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(normalizeLanguage(response.body().getResult()));
                    return;
                }

                callback.onError(appContext.getString(R.string.error_generic));
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "getLanguage failed", t);
                callback.onError(appContext.getString(R.string.error_network_unknown));
            }
        });
    }

    public void updateLanguage(String authHeader, String locale,
                               SettingsViewModel.SettingsUpdateCallback callback) {
        apiService.updateLanguage(authHeader, locale).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }

                if (callback != null) {
                    callback.onError(extractErrorMessage(
                            response,
                            appContext.getString(R.string.error_generic)
                    ));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "updateLanguage failed", t);
                if (callback != null) {
                    callback.onError(appContext.getString(R.string.error_network_unknown));
                }
            }
        });
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

    private String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        return language.trim().toLowerCase();
    }
}
