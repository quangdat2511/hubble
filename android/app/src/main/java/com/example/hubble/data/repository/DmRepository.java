package com.example.hubble.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.UpdateMessageRequest;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.lang.reflect.Type;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DmRepository {

    private static final String DM_PREFS = "dm_prefs";
    private static final String OPENED_DM_CHANNELS_PREFIX = "opened_dm_channels_";

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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(
                            result != null ? result : new ArrayList<>())
                    );
                    return;
                }

                // Backward-compatible fallback in case backend is still on old contacts route.
                apiService.getFriendsViaContacts(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<FriendUserDto>>> call,
                                           Response<ApiResponse<List<FriendUserDto>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<FriendUserDto> result = response.body().getResult();
                            callback.onResult(com.example.hubble.data.model.AuthResult.success(
                                    result != null ? result : new ArrayList<>())
                            );
                            return;
                        }
                        callback.onResult(com.example.hubble.data.model.AuthResult.error(
                                extractErrorMessage(response, "Không tải được danh sách bạn bè")
                        ));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                        callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
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
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không tải được danh sách DM"));
            }

            @Override
            public void onFailure(Call<List<ChannelDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không tạo được kênh DM"));
            }

            @Override
            public void onFailure(Call<ChannelDto> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không tải được tin nhắn"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MessageDto>>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    public void sendMessage(String channelId, String content, RepositoryCallback<MessageDto> callback) {
        sendMessage(channelId, null, content, callback);
    }

    public void sendMessage(String channelId, String replyToId, String content, RepositoryCallback<MessageDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        CreateMessageRequest request = new CreateMessageRequest(channelId, replyToId, content);
        apiService.sendMessage(token, request).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call, Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không gửi được tin nhắn"));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không chỉnh sửa được tin nhắn"));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Không thu hồi được tin nhắn"));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Lỗi mạng: " + t.getMessage()));
            }
        });
    }

    public String getCurrentUserId() {
        if (tokenManager.getUser() == null) {
            return null;
        }
        return tokenManager.getUser().getId();
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
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            currentUserId = "anonymous";
        }
        return OPENED_DM_CHANNELS_PREFIX + currentUserId;
    }

    private <T> String requireAuthToken(RepositoryCallback<T> callback) {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(com.example.hubble.data.model.AuthResult.error("Bạn chưa đăng nhập"));
            return null;
        }
        return "Bearer " + accessToken;
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
            return "Phiên đăng nhập hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.";
        }
        return fallback + " (HTTP " + httpCode + ")";
    }
}
