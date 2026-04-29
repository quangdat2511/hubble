package com.example.hubble.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.hubble.R;
import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
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
    private final Context appContext;

    public FriendRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(appContext);
        this.gson = new Gson();
    }

    private String getToken() {
        String token = tokenManager.getAccessToken();
        return (token != null && !token.isEmpty()) ? "Bearer " + token : null;
    }

    public void searchUsers(String query, RepositoryCallback<List<FriendUserDto>> callback) {
        String token = getToken();
        if (token == null) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_unauthorized)));
            return;
        }
        apiService.searchUsers(token, query).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Response<ApiResponse<List<FriendUserDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_search_error)));
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
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_request_send_error)));
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
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_list_load_error)));
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
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_accept_error)));
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
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_action_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void getOutgoingRequests(RepositoryCallback<List<FriendRequestResponse>> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.getOutgoingRequests(token).enqueue(new Callback<ApiResponse<List<FriendRequestResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendRequestResponse>>> call, @NonNull Response<ApiResponse<List<FriendRequestResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_list_load_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendRequestResponse>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void sendRequestById(String userId, RepositoryCallback<FriendRequestResponse> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.sendFriendRequest(token, userId).enqueue(new Callback<ApiResponse<FriendRequestResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<FriendRequestResponse>> call, @NonNull Response<ApiResponse<FriendRequestResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_request_send_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<FriendRequestResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void getBlockedUsers(RepositoryCallback<List<FriendUserDto>> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.getBlockedUsers(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Response<ApiResponse<List<FriendUserDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_list_load_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void blockUser(String userId, RepositoryCallback<String> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.blockUser(token, userId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_block_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void unblockUser(String userId, RepositoryCallback<String> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.unblockUser(token, userId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_unblock_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void getFriends(RepositoryCallback<List<FriendUserDto>> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.getFriends(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Response<ApiResponse<List<FriendUserDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_list_load_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendUserDto>>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

    public void unfriend(String userId, RepositoryCallback<String> callback) {
        String token = getToken();
        if (token == null) return;
        apiService.unfriend(token, userId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error(appContext.getString(R.string.friend_unfriend_error)));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        });
    }

}

