package com.example.hubble.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.CreateServerRequest;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.server.ServerResponse;
import com.example.hubble.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerRepository {

    private int colorIndex = 0;

    private final Context appContext;
    private final TokenManager tokenManager;
    private final int[] defaultColors;

    public ServerRepository(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
        defaultColors = resolveDefaultColors();
    }

    public void createServer(String name, String type, RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }
        String token = "Bearer " + accessToken;
        CreateServerRequest request = new CreateServerRequest(name, type);

        RetrofitClient.getServerService(appContext).createServer(token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ServerResponse>> call,
                                   @NonNull Response<ApiResponse<ServerResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    ServerResponse sr = response.body().getResult();
                    int color = defaultColors[colorIndex % defaultColors.length];
                    colorIndex++;
                    ServerItem item = new ServerItem(sr.getId(), sr.getName(), sr.getIconUrl(), color);
                    callback.onResult(AuthResult.success(item));
                } else {
                    String error = response.body() != null ? response.body().getMessage() : null;
                    callback.onResult(AuthResult.error(error != null ? error : appContext.getString(R.string.server_create_error)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ServerResponse>> call, @NonNull Throwable t) {
                callback.onResult(AuthResult.error(appContext.getString(R.string.connection_error, t.getMessage())));
            }
        });
    }

    public void getMyServers(RepositoryCallback<List<ServerItem>> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }

        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).getMyServers(token)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerResponse>>> response) {
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
                        callback.onResult(AuthResult.error(error != null ? error : appContext.getString(R.string.server_list_load_error)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error(appContext.getString(R.string.connection_error, t.getMessage())));
                    }
                });
    }

    public void getServerChannels(String serverId, RepositoryCallback<List<ChannelDto>> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }

        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).getServerChannels(token, serverId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelDto>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                            return;
                        }

                        String error = response.body() != null ? response.body().getMessage() : null;
                        callback.onResult(AuthResult.error(error != null ? error : appContext.getString(R.string.server_channels_load_error)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelDto>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error(appContext.getString(R.string.connection_error, t.getMessage())));
                    }
                });
    }

    private ServerItem mapToServerItem(ServerResponse server, int index) {
        int color = defaultColors[index % defaultColors.length];
        return new ServerItem(server.getId(), server.getName(), server.getIconUrl(), color);
    }

    private int[] resolveDefaultColors() {
        return new int[] {
                ContextCompat.getColor(appContext, R.color.color_primary),
                ContextCompat.getColor(appContext, R.color.server_color_green),
                ContextCompat.getColor(appContext, R.color.server_color_yellow),
                ContextCompat.getColor(appContext, R.color.server_color_red),
                ContextCompat.getColor(appContext, R.color.server_color_pink)
        };
    }
}
