package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerInviteService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.CreateInviteRequest;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerInviteRepository {

    private final Context appContext;
    private final TokenManager tokenManager;

    public ServerInviteRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
    }

    public void createInvite(String serverId, String inviteeUsername,
                             RepositoryCallback<ServerInviteResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        ServerInviteService service = RetrofitClient.getServerInviteService(appContext);
        service.createInvite(token, serverId, new CreateInviteRequest(inviteeUsername))
                .enqueue(new Callback<ApiResponse<ServerInviteResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                           @NonNull Response<ApiResponse<ServerInviteResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : null;
                            callback.onResult(AuthResult.error(
                                    msg != null ? msg : "Không thể gửi lời mời"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void getServerInvites(String serverId,
                                  RepositoryCallback<List<ServerInviteResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getServerInviteService(appContext)
                .getServerInvites(token, serverId)
                .enqueue(new Callback<ApiResponse<List<ServerInviteResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerInviteResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerInviteResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : null;
                            callback.onResult(AuthResult.error(
                                    msg != null ? msg : "Không tải được danh sách lời mời"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerInviteResponse>>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void getMyInvites(RepositoryCallback<List<ServerInviteResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getServerInviteService(appContext)
                .getMyInvites(token)
                .enqueue(new Callback<ApiResponse<List<ServerInviteResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerInviteResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerInviteResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : null;
                            callback.onResult(AuthResult.error(
                                    msg != null ? msg : "Không tải được lời mời của bạn"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerInviteResponse>>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void acceptInvite(String inviteId,
                              RepositoryCallback<ServerInviteResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getServerInviteService(appContext)
                .acceptInvite(token, inviteId)
                .enqueue(new Callback<ApiResponse<ServerInviteResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                           @NonNull Response<ApiResponse<ServerInviteResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ServerInviteResponse result = response.body().getResult();
                            callback.onResult(AuthResult.success(
                                    result != null ? result : new ServerInviteResponse()));
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : null;
                            callback.onResult(AuthResult.error(
                                    msg != null ? msg : "Không thể chấp nhận lời mời"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void declineInvite(String inviteId,
                               RepositoryCallback<ServerInviteResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getServerInviteService(appContext)
                .declineInvite(token, inviteId)
                .enqueue(new Callback<ApiResponse<ServerInviteResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                           @NonNull Response<ApiResponse<ServerInviteResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ServerInviteResponse result = response.body().getResult();
                            callback.onResult(AuthResult.success(
                                    result != null ? result : new ServerInviteResponse()));
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : null;
                            callback.onResult(AuthResult.error(
                                    msg != null ? msg : "Không thể từ chối lời mời"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerInviteResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    private String bearerToken() {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) return null;
        return "Bearer " + accessToken;
    }
}

