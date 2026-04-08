package com.example.hubble.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.CreateChannelRequest;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.server.ServerResponse;
import com.example.hubble.utils.TokenManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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

    public void createServer(String name, @Nullable Uri iconUri,
                             RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return;
        }

        String token = "Bearer " + accessToken;
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
        MultipartBody.Part iconPart = iconUri != null ? buildFilePart("icon", iconUri) : null;

        RetrofitClient.getServerService(appContext)
                .createServer(token, namePart, iconPart)
                .enqueue(new Callback<ApiResponse<ServerResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerResponse>> call,
                                           @NonNull Response<ApiResponse<ServerResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            ServerResponse server = response.body().getResult();
                            int mappedIndex = colorIndex++;
                            callback.onResult(AuthResult.success(mapToServerItem(server, mappedIndex)));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerResponse>> call,
                                          @NonNull Throwable t) {
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
                .enqueue(new Callback<ApiResponse<List<ServerResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            List<ServerItem> items = new ArrayList<>();
                            List<ServerResponse> servers = response.body().getResult();
                            for (int i = 0; i < servers.size(); i++) {
                                items.add(mapToServerItem(servers.get(i), i));
                            }
                            callback.onResult(AuthResult.success(items));
                            return;
                        }

                        callback.onResult(AuthResult.error(resolveError(response)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerResponse>>> call,
                                          @NonNull Throwable t) {
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
                .enqueue(new Callback<ApiResponse<List<ChannelDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelDto>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                            return;
                        }

                        callback.onResult(AuthResult.error(resolveError(response)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error(appContext.getString(R.string.connection_error, t.getMessage())));
                    }
                });
    }

    public void updateServerIcon(String serverId, Uri iconUri,
                                 RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Báº¡n chÆ°a Ä‘Äƒng nháº­p"));
            return;
        }
        MultipartBody.Part iconPart = buildFilePart("icon", iconUri);
        if (iconPart == null) {
            callback.onResult(AuthResult.error("KhÃ´ng Ä‘á»c Ä‘Æ°á»£c file áº£nh"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext)
                .updateServerIcon(token, serverId, iconPart)
                .enqueue(new Callback<ApiResponse<ServerResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerResponse>> call,
                                           @NonNull Response<ApiResponse<ServerResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(
                                    mapToServerItem(response.body().getResult(), colorIndex)));
                        } else {
                            callback.onResult(AuthResult.error(resolveIconError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lá»—i káº¿t ná»‘i: " + t.getMessage()));
                    }
                });
    }

    public void deleteServerIcon(String serverId, RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Báº¡n chÆ°a Ä‘Äƒng nháº­p"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext)
                .deleteServerIcon(token, serverId)
                .enqueue(new Callback<ApiResponse<ServerResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerResponse>> call,
                                           @NonNull Response<ApiResponse<ServerResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(
                                    mapToServerItem(response.body().getResult(), colorIndex)));
                        } else {
                            callback.onResult(AuthResult.error(resolveIconError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lá»—i káº¿t ná»‘i: " + t.getMessage()));
                    }
                });
    }

    public void updateServer(String serverId, String name, String description,
                            RepositoryCallback<ServerResponse> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }
        String token = "Bearer " + accessToken;
        Map<String, String> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);

        RetrofitClient.getServerService(appContext)
                .updateServer(token, serverId, body)
                .enqueue(new Callback<ApiResponse<ServerResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ServerResponse>> call,
                                           @NonNull Response<ApiResponse<ServerResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ServerResponse>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void deleteServer(String serverId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).deleteServer(token, serverId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void kickMember(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Báº¡n chÆ°a Ä‘Äƒng nháº­p"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).kickMember(token, serverId, memberId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lá»—i káº¿t ná»‘i: " + t.getMessage()));
                    }
                });
    }

    public void transferOwnership(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Báº¡n chÆ°a Ä‘Äƒng nháº­p"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext).transferOwnership(token, serverId, memberId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lá»—i káº¿t ná»‘i: " + t.getMessage()));
                    }
                });
    }

    private ServerItem mapToServerItem(ServerResponse server, int index) {
        int color = defaultColors[index % defaultColors.length];
        return new ServerItem(server.getId(), server.getOwnerId(),
                server.getName(), server.getDescription(), server.getIconUrl(), color);
    }

    @Nullable
    private MultipartBody.Part buildFilePart(String fieldName, Uri uri) {
        try (InputStream inputStream = appContext.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            String mimeType = appContext.getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";
            String ext = mimeType.contains("/") ? mimeType.split("/")[1] : "jpg";
            RequestBody body = RequestBody.create(MediaType.parse(mimeType), buffer.toByteArray());
            return MultipartBody.Part.createFormData(fieldName, "icon." + ext, body);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> String resolveError(Response<ApiResponse<T>> response) {
        if (response.body() != null) {
            int code = response.body().getCode();
            String msg = response.body().getMessage();
            if (code == 3013) return "TÃªn mÃ¡y chá»§ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng";
            if (code == 3012) return "Báº¡n khÃ´ng pháº£i chá»§ sá»Ÿ há»¯u mÃ¡y chá»§";
            if (code == 3001) return "MÃ¡y chá»§ khÃ´ng tá»“n táº¡i";
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return "ÄÃ£ xáº£y ra lá»—i. Vui lÃ²ng thá»­ láº¡i.";
    }

    private <T> String resolveIconError(Response<ApiResponse<T>> response) {
        if (response.body() != null) {
            int code = response.body().getCode();
            if (code == 6002) return "Chá»‰ cháº¥p nháº­n áº£nh (jpg, png, gif, webp, svg)";
            if (code == 6003) return "áº¢nh pháº£i nhá» hÆ¡n 5 MB";
            if (code == 6001) return "Táº£i áº£nh tháº¥t báº¡i. Vui lÃ²ng thá»­ láº¡i.";
            if (code == 3012) return "Báº¡n khÃ´ng pháº£i chá»§ sá»Ÿ há»¯u mÃ¡y chá»§";
            if (code == 3001) return "MÃ¡y chá»§ khÃ´ng tá»“n táº¡i";
            String msg = response.body().getMessage();
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return "ÄÃ£ xáº£y ra lá»—i. Vui lÃ²ng thá»­ láº¡i.";
    }

    private int[] resolveDefaultColors() {
        return new int[]{
                ContextCompat.getColor(appContext, R.color.color_primary),
                ContextCompat.getColor(appContext, R.color.server_color_green),
                ContextCompat.getColor(appContext, R.color.server_color_yellow),
                ContextCompat.getColor(appContext, R.color.server_color_red),
                ContextCompat.getColor(appContext, R.color.server_color_pink)
        };
    }

    // ── Create channel ────────────────────────────────────────────────────

    public void createChannel(String serverId, CreateChannelRequest request,
                              RepositoryCallback<ChannelDto> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }
        String token = "Bearer " + accessToken;
        RetrofitClient.getServerService(appContext)
                .createChannel(token, serverId, request)
                .enqueue(new Callback<ApiResponse<ChannelDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChannelDto>> call,
                                           @NonNull Response<ApiResponse<ChannelDto>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error(resolveError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChannelDto>> call,
                                         @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }
}
