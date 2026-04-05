package com.example.hubble.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hubble.R;
import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
// Chú ý: Dùng duy nhất đường dẫn AuthResult chuẩn này
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.MarkChannelReadRequest;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.PeerReadStatusDto;
import com.example.hubble.data.model.dm.ReactionDto;
import com.example.hubble.data.model.dm.UpdateMessageRequest;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DmRepository {

    private static final String DM_PREFS = "dm_prefs";
    private static final String OPENED_DM_CHANNELS_PREFIX = "opened_dm_channels_";
    private static final String FAVORITE_DM_CHANNELS_PREFIX = "favorite_dm_channels_";

    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Gson gson;
    private final Context appContext;

    public DmRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(context.getApplicationContext());
        this.gson = new Gson();
    }

    public void rememberOpenedDirectChannel(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }

        Set<String> openedChannels = readOpenedDmChannels();
        if (openedChannels.add(channelId)) {
            saveOpenedDmChannels(openedChannels);
        }
    }

    public Set<String> getLocallyOpenedDirectChannelIds() {
        return readOpenedDmChannels();
    }

    public void pruneLocallyOpenedDirectChannels(Set<String> validChannelIds) {
        if (validChannelIds == null) {
            return;
        }

        Set<String> openedChannels = readOpenedDmChannels();
        if (openedChannels.retainAll(new HashSet<>(validChannelIds))) {
            saveOpenedDmChannels(openedChannels);
        }
    }

    public Set<String> getFavoriteDirectChannelIds() {
        return readFavoriteDmChannels();
    }

    public void setDirectChannelFavorite(String channelId, boolean favorite) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }

        Set<String> favoriteChannels = readFavoriteDmChannels();
        boolean changed;
        if (favorite) {
            changed = favoriteChannels.add(channelId);
        } else {
            changed = favoriteChannels.remove(channelId);
        }

        if (changed) {
            saveFavoriteDmChannels(favoriteChannels);
        }
    }

    public void pruneFavoriteDirectChannels(Set<String> validChannelIds) {
        if (validChannelIds == null) {
            return;
        }

        Set<String> favoriteChannels = readFavoriteDmChannels();
        if (favoriteChannels.retainAll(new HashSet<>(validChannelIds))) {
            saveFavoriteDmChannels(favoriteChannels);
        }
    }

    public void getFriends(RepositoryCallback<List<FriendUserDto>> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getFriends(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FriendUserDto>>> call,
                                   Response<ApiResponse<List<FriendUserDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendUserDto> result = response.body().getResult();
                    callback.onResult(AuthResult.success(
                            result != null ? result : new ArrayList<>())
                    );
                    return;
                }

                apiService.getFriendsViaContacts(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<FriendUserDto>>> call,
                                           Response<ApiResponse<List<FriendUserDto>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<FriendUserDto> result = response.body().getResult();
                            callback.onResult(AuthResult.success(
                                    result != null ? result : new ArrayList<>())
                            );
                            return;
                        }
                        callback.onResult(AuthResult.error(
                                extractErrorMessage(response, appContext.getString(R.string.dm_friends_load_error))
                        ));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                        callback.onResult(AuthResult.error(buildNetworkError(t)));
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public void getDirectChannels(RepositoryCallback<List<ChannelDto>> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getDirectChannels(token).enqueue(new Callback<List<ChannelDto>>() {
            @Override
            public void onResponse(Call<List<ChannelDto>> call, Response<List<ChannelDto>> response) {
                if (response.isSuccessful()) {
                    List<ChannelDto> body = response.body();
                    callback.onResult(AuthResult.success(body != null ? body : new ArrayList<>()));
                    return;
                }
                callback.onResult(AuthResult.error(
                        extractErrorMessage(response, appContext.getString(R.string.dm_list_load_error))));
            }

            @Override
            public void onFailure(Call<List<ChannelDto>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public void getOrCreateDirectChannel(String otherUserId, RepositoryCallback<ChannelDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getOrCreateDirectChannel(token, otherUserId).enqueue(new Callback<ChannelDto>() {
            @Override
            public void onResponse(Call<ChannelDto> call, Response<ChannelDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body()));
                    return;
                }
                callback.onResult(AuthResult.error(appContext.getString(R.string.dm_channel_create_error)));
            }

            @Override
            public void onFailure(Call<ChannelDto> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public void getMyQrToken(RepositoryCallback<String> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getMyQrToken(token).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(
                        extractErrorMessage(response, "Khong tai duoc ma QR")
                ));
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Loi mang: " + t.getMessage()));
            }
        });
    }

    public void scanQrProfile(String qrToken, RepositoryCallback<UserResponse> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.scanQrProfile(token, qrToken).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call,
                                   Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(
                        extractErrorMessage(response, "Khong tai duoc ho so tu QR")
                ));
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Loi mang: " + t.getMessage()));
            }
        });
    }

    public void markChannelRead(String channelId, String messageId, RepositoryCallback<Void> callback) {
        String token = requireAuthToken(callback);
        if (token == null) return;
        if (channelId == null || messageId == null) {
            callback.onResult(AuthResult.error("Thiếu thông tin"));
            return;
        }
        apiService.markChannelRead(token, channelId, new MarkChannelReadRequest(messageId))
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                            return;
                        }
                        callback.onResult(AuthResult.error(extractErrorMessage(response, "Không cập nhật được trạng thái đã đọc")));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi mạng: " + t.getMessage()));
                    }
                });
    }

    public void getPeerReadStatus(String channelId, RepositoryCallback<PeerReadStatusDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) return;
        apiService.getPeerReadStatus(token, channelId).enqueue(new Callback<ApiResponse<PeerReadStatusDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<PeerReadStatusDto>> call,
                                   Response<ApiResponse<PeerReadStatusDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(extractErrorMessage(response, "Không tải được trạng thái đã xem")));
            }

            @Override
            public void onFailure(Call<ApiResponse<PeerReadStatusDto>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    public void getMessages(String channelId, int page, int size, RepositoryCallback<List<MessageDto>> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.getMessages(token, channelId, page, size).enqueue(new Callback<ApiResponse<List<MessageDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MessageDto>>> call,
                                   Response<ApiResponse<List<MessageDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(appContext.getString(R.string.dm_messages_load_error)));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MessageDto>>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    // Gộp tất cả SendMessage cũ thành 1 hàm duy nhất nhận đủ tham số
    public void sendMessage(String channelId, String replyToId, String content,
                            List<String> attachmentIds, String type,
                            RepositoryCallback<MessageDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) return;

        CreateMessageRequest request = new CreateMessageRequest(
                channelId, replyToId, content, type, attachmentIds
        );

        apiService.sendMessage(token, request).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call,
                                   Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(appContext.getString(R.string.dm_send_error)));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    // Giữ lại hàm rút gọn (Overload) để không bị lỗi ở các màn hình khác
    public void sendMessage(String channelId, String replyToId, String content, RepositoryCallback<MessageDto> callback) {
        sendMessage(channelId, replyToId, content, new ArrayList<>(), "TEXT", callback);
    }

    public void sendMessage(String channelId, String content, RepositoryCallback<MessageDto> callback) {
        sendMessage(channelId, null, content, new ArrayList<>(), "TEXT", callback);
    }

    public void editMessage(String messageId, String content, RepositoryCallback<MessageDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        UpdateMessageRequest request = new UpdateMessageRequest(content);
        apiService.editMessage(token, messageId, request).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call, Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(appContext.getString(R.string.dm_edit_error)));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public void toggleReaction(String messageId, String emoji, RepositoryCallback<List<ReactionDto>> callback) {
        String token = requireAuthToken(callback);
        if (token == null) return;

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("emoji", emoji);

        apiService.toggleReaction(token, messageId, body).enqueue(new Callback<ApiResponse<List<ReactionDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReactionDto>>> call,
                                   Response<ApiResponse<List<ReactionDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error("Không thể thả cảm xúc"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ReactionDto>>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    public void unsendMessage(String messageId, RepositoryCallback<MessageDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.unsendMessage(token, messageId).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call, Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(AuthResult.error(appContext.getString(R.string.dm_unsend_error)));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(AuthResult.error(buildNetworkError(t)));
            }
        });
    }

    public String getCurrentUserId() {
        if (tokenManager.getUser() == null) {
            return null;
        }
        return tokenManager.getUser().getId();
    }

    public String getAccessTokenRaw() {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return null;
        }
        return accessToken;
    }

    private Set<String> readOpenedDmChannels() {
        SharedPreferences prefs = appContext.getSharedPreferences(DM_PREFS, Context.MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(openedDmStorageKey(), new HashSet<>());
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    private void saveOpenedDmChannels(Set<String> channelIds) {
        SharedPreferences prefs = appContext.getSharedPreferences(DM_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(openedDmStorageKey(), new HashSet<>(channelIds)).apply();
    }

    private String openedDmStorageKey() {
        return OPENED_DM_CHANNELS_PREFIX + currentUserStorageKeySuffix();
    }

    private Set<String> readFavoriteDmChannels() {
        SharedPreferences prefs = appContext.getSharedPreferences(DM_PREFS, Context.MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(favoriteDmStorageKey(), new HashSet<>());
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    private void saveFavoriteDmChannels(Set<String> channelIds) {
        SharedPreferences prefs = appContext.getSharedPreferences(DM_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(favoriteDmStorageKey(), new HashSet<>(channelIds)).apply();
    }

    private String favoriteDmStorageKey() {
        return FAVORITE_DM_CHANNELS_PREFIX + currentUserStorageKeySuffix();
    }

    private String currentUserStorageKeySuffix() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            currentUserId = "anonymous";
        }
        return currentUserId;
    }

    private <T> String requireAuthToken(RepositoryCallback<T> callback) {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(AuthResult.error(appContext.getString(R.string.error_not_logged_in)));
            return null;
        }
        return "Bearer " + accessToken;
    }

    private String buildNetworkError(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            message = appContext.getString(R.string.error_network_unknown);
        }
        return appContext.getString(R.string.error_network, message);
    }

    private <T> String extractErrorMessage(Response<T> response, String fallback) {
        if (response == null) {
            return fallback;
        }

        int httpCode = response.code();
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().trim().isEmpty()) {
                        return apiError.getMessage() + " (HTTP " + httpCode + ")";
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }

        if (httpCode == 401 || httpCode == 403) {
            return appContext.getString(R.string.auth_session_expired);
        }
        return fallback + " (HTTP " + httpCode + ")";
    }
}

