package com.example.hubble.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {
    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Gson gson;

    public FriendRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(context.getApplicationContext());
        this.gson = new Gson();
    }

    private String getToken() {
        String token = tokenManager.getAccessToken();
        return (token != null && !token.isEmpty()) ? "Bearer " + token : null;
    }

    public void searchUsers(String query, RepositoryCallback<List<FriendUserDto>> callback) {
        String token = getToken();
        if (token == null) {
            callback.onResult(AuthResult.error("Unauthorized"));
            return;
        }
        apiService.searchUsers(token, query).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Response<ApiResponse<List<FriendUserDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi tìm kiếm"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void sendRequestByUsername(String username, RepositoryCallback<FriendRequestResponse> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.sendFriendRequestByUsername(token, username).enqueue(new Callback<ApiResponse<FriendRequestResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<FriendRequestResponse>> call, @NonNull Response<ApiResponse<FriendRequestResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Không thể gửi lời mời"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<FriendRequestResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void getIncomingRequests(RepositoryCallback<List<FriendRequestResponse>> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.getIncomingRequests(token).enqueue(new Callback<ApiResponse<List<FriendRequestResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendRequestResponse>>> call, @NonNull Response<ApiResponse<List<FriendRequestResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi tải danh sách"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendRequestResponse>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void acceptRequest(String requestId, RepositoryCallback<String> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.acceptRequest(token, requestId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi chấp nhận"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void declineRequest(String requestId, RepositoryCallback<String> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.declineRequest(token, requestId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi từ chối"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }
}