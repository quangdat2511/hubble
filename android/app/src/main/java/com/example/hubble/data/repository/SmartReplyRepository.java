package com.example.hubble.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.SmartReplyResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmartReplyRepository {

    private final ApiService apiService;

    public SmartReplyRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public LiveData<SmartReplyResult> fetchSmartReply(String content) {
        MutableLiveData<SmartReplyResult> result = new MutableLiveData<>();

        // Post loading state immediately
        result.postValue(SmartReplyResult.loading());

        Map<String, String> body = new HashMap<>();
        body.put("content", content);

        apiService.getSmartReply(body).enqueue(new retrofit2.Callback<SmartReplyResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SmartReplyResponse> call, retrofit2.Response<SmartReplyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lấy thẳng response.body() thay vì getResult()
                    result.postValue(SmartReplyResult.success(response.body()));
                } else {
                    android.util.Log.e("SmartReply", "Lỗi Server: " + response.code());
                    result.postValue(SmartReplyResult.error("Server error: " + response.code()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SmartReplyResponse> call, Throwable t) {
                android.util.Log.e("SmartReply", "Lỗi Mạng: " + t.getMessage());
                result.postValue(SmartReplyResult.error("Network error: " + t.getMessage()));
            }
        });

        return result;
    }

    // --- Result wrapper ---
    public static class SmartReplyResult {
        public enum Status { LOADING, SUCCESS, ERROR, IDLE }

        public final Status status;
        public final SmartReplyResponse data;
        public final String errorMessage;

        private SmartReplyResult(Status status, SmartReplyResponse data, String errorMessage) {
            this.status = status;
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public static SmartReplyResult loading() {
            return new SmartReplyResult(Status.LOADING, null, null);
        }

        public static SmartReplyResult success(SmartReplyResponse data) {
            return new SmartReplyResult(Status.SUCCESS, data, null);
        }

        public static SmartReplyResult error(String message) {
            return new SmartReplyResult(Status.ERROR, null, message);
        }

        public static SmartReplyResult idle() {
            return new SmartReplyResult(Status.IDLE, null, null);
        }
    }
}