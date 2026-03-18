package com.example.hubble.data.repository;

import android.content.Context;

import android.graphics.Color;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.CreateServerRequest;
import com.example.hubble.data.model.ServerItem;
import com.example.hubble.data.model.ServerResponse;
import com.example.hubble.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerRepository {

    private static final int[] DEFAULT_COLORS = {
            Color.parseColor("#5865F2"),
            Color.parseColor("#57F287"),
            Color.parseColor("#FEE75C"),
            Color.parseColor("#ED4245"),
            Color.parseColor("#EB459E"),
    };
    private int colorIndex = 0;

    private final Context appContext;
    private final TokenManager tokenManager;

    public ServerRepository(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
    }

    public void createServer(String name, String type, RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }
        String token = "Bearer " + accessToken;
        CreateServerRequest request = new CreateServerRequest(name, type);

        RetrofitClient.getServerService(appContext).createServer(token, request).enqueue(new Callback<ApiResponse<ServerResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ServerResponse>> call, Response<ApiResponse<ServerResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    ServerResponse sr = response.body().getResult();
                    int color = DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length];
                    colorIndex++;
                    ServerItem item = new ServerItem(sr.getId(), sr.getName(), sr.getIconUrl(), color);
                    callback.onResult(AuthResult.success(item));
                } else {
                    String error = response.body() != null ? response.body().getMessage() : null;
                    callback.onResult(AuthResult.error(error != null ? error : "Tạo máy chủ thất bại"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ServerResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void getMyServers(RepositoryCallback<List<ServerItem>> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }

        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).getMyServers(token)
                .enqueue(new Callback<ApiResponse<List<ServerResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<ServerResponse>>> call,
                                           Response<ApiResponse<List<ServerResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                            List<ServerItem> items = new ArrayList<>();
                            List<ServerResponse> servers = response.body().getResult();
                            for (int i = 0; i < servers.size(); i++) {
                                items.add(mapToServerItem(servers.get(i), i));
                            }
                            callback.onResult(AuthResult.success(items));
                            return;
                        }

                        String error = response.body() != null ? response.body().getMessage() : null;
                        callback.onResult(AuthResult.error(error != null ? error : "Không tải được danh sách máy chủ"));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<ServerResponse>>> call, Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    private ServerItem mapToServerItem(ServerResponse server, int index) {
        int color = DEFAULT_COLORS[index % DEFAULT_COLORS.length];
        return new ServerItem(server.getId(), server.getName(), server.getIconUrl(), color);
    }
}
