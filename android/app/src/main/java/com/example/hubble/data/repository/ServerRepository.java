package com.example.hubble.data.repository;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerService;
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
import java.util.List;

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

    // ── Create server (multipart) ─────────────────────────────────────────

    public void createServer(String name, @Nullable Uri iconUri,
                             RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
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
                            ServerResponse sr = response.body().getResult();
                            int color = defaultColors[colorIndex % defaultColors.length];
                            colorIndex++;
                            callback.onResult(AuthResult.success(mapToServerItem(sr, colorIndex - 1)));
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

    // ── Get my servers ────────────────────────────────────────────────────

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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Server channels ───────────────────────────────────────────────────

    public void getServerChannels(String serverId, RepositoryCallback<List<ChannelDto>> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Icon management ───────────────────────────────────────────────────

    public void updateServerIcon(String serverId, Uri iconUri,
                                 RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
            return;
        }
        MultipartBody.Part iconPart = buildFilePart("icon", iconUri);
        if (iconPart == null) {
            callback.onResult(AuthResult.error("Không đọc được file ảnh"));
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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void deleteServerIcon(String serverId, RepositoryCallback<ServerItem> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Member management ─────────────────────────────────────────────────

    public void kickMember(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    public void transferOwnership(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error("Bạn chưa đăng nhập"));
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
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ServerItem mapToServerItem(ServerResponse server, int index) {
        int color = defaultColors[index % defaultColors.length];
        return new ServerItem(server.getId(), server.getOwnerId(),
                server.getName(), server.getIconUrl(), color);
    }

    /** Build a multipart file part from a content URI. Returns null on failure. */
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

    /** Map HTTP error codes to user-friendly messages per §2 of the API contract. */
    private <T> String resolveError(Response<ApiResponse<T>> response) {
        if (response.body() != null) {
            int code = response.body().getCode();
            String msg = response.body().getMessage();
            if (code == 3013) return "Tên máy chủ không được để trống";
            if (code == 3012) return "Bạn không phải chủ sở hữu máy chủ";
            if (code == 3001) return "Máy chủ không tồn tại";
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return "Đã xảy ra lỗi. Vui lòng thử lại.";
    }

    private <T> String resolveIconError(Response<ApiResponse<T>> response) {
        if (response.body() != null) {
            int code = response.body().getCode();
            if (code == 6002) return "Chỉ chấp nhận ảnh (jpg, png, gif, webp, svg)";
            if (code == 6003) return "Ảnh phải nhỏ hơn 5 MB";
            if (code == 6001) return "Tải ảnh thất bại. Vui lòng thử lại.";
            if (code == 3012) return "Bạn không phải chủ sở hữu máy chủ";
            if (code == 3001) return "Máy chủ không tồn tại";
            String msg = response.body().getMessage();
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return "Đã xảy ra lỗi. Vui lòng thử lại.";
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

