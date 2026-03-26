package com.example.hubble.data.repository;

import android.content.Context;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DmRepository {

    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Gson gson;

    public DmRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(context.getApplicationContext());
        this.gson = new Gson();
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
                            result != null ? result : new ArrayList<>()));
                    return;
                }

                apiService.getFriendsViaContacts(token).enqueue(new Callback<ApiResponse<List<FriendUserDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<FriendUserDto>>> call,
                                           Response<ApiResponse<List<FriendUserDto>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<FriendUserDto> result = response.body().getResult();
                            callback.onResult(com.example.hubble.data.model.AuthResult.success(
                                    result != null ? result : new ArrayList<>()));
                            return;
                        }
                        callback.onResult(com.example.hubble.data.model.AuthResult.error(
                                extractErrorMessage(response, "Khong tai duoc danh sach ban be")
                        ));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                        callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FriendUserDto>>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
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
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error(
                        extractErrorMessage(response, "Khong tai duoc ma QR")
                ));
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
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
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error(
                        extractErrorMessage(response, "Khong doc duoc ma QR")
                ));
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
            }
        });
    }

    public void sendFriendRequest(String userId, RepositoryCallback<String> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        apiService.sendFriendRequest(token, userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(com.example.hubble.data.model.AuthResult.success("Da gui loi moi ket ban"));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error(
                        extractErrorMessage(response, "Khong gui duoc loi moi ket ban")
                ));
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
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
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Khong tai duoc danh sach DM"));
            }

            @Override
            public void onFailure(Call<List<ChannelDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
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
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Khong tao duoc kenh DM"));
            }

            @Override
            public void onFailure(Call<ChannelDto> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
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
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Khong tai duoc tin nhan"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MessageDto>>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
            }
        });
    }

    public void sendMessage(String channelId, String content, RepositoryCallback<MessageDto> callback) {
        String token = requireAuthToken(callback);
        if (token == null) {
            return;
        }

        CreateMessageRequest request = new CreateMessageRequest(channelId, null, content);
        apiService.sendMessage(token, request).enqueue(new Callback<ApiResponse<MessageDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageDto>> call, Response<ApiResponse<MessageDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(com.example.hubble.data.model.AuthResult.success(response.body().getResult()));
                    return;
                }
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Khong gui duoc tin nhan"));
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageDto>> call, Throwable t) {
                callback.onResult(com.example.hubble.data.model.AuthResult.error("Loi mang: " + t.getMessage()));
            }
        });
    }

    public String getCurrentUserId() {
        if (tokenManager.getUser() == null) {
            return null;
        }
        return tokenManager.getUser().getId();
    }

    private <T> String requireAuthToken(RepositoryCallback<T> callback) {
        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onResult(com.example.hubble.data.model.AuthResult.error("Ban chua dang nhap"));
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
        }

        if (httpCode == 401 || httpCode == 403) {
            return "Phien dang nhap het han hoac khong hop le. Vui long dang nhap lai.";
        }
        return fallback + " (HTTP " + httpCode + ")";
    }
}
