package com.example.hubble.data.repository;

import android.content.Context;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.search.PagedResponse;
import com.example.hubble.data.model.search.SearchAttachmentDto;
import com.example.hubble.data.model.search.SearchChannelDto;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.data.model.search.SearchMessageDto;
import com.example.hubble.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchRepository {

    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final Context appContext;

    public SearchRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(context);
        this.tokenManager = new TokenManager(appContext);
    }

    private String getToken() {
        String token = tokenManager.getAccessToken();
        return (token != null && !token.isEmpty()) ? "Bearer " + token : null;
    }

    // ── Context window ────────────────────────────────────────────────────

    public void getMessagesAround(String channelId, String messageId, int limit,
                                  RepositoryCallback<List<MessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.getMessagesAround(token, channelId, messageId, limit)
                .enqueue(new Callback<ApiResponse<List<MessageDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<MessageDto>>> call,
                                           Response<ApiResponse<List<MessageDto>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onResult(AuthResult.success(response.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Failed to load context"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<MessageDto>>> call, Throwable t) {
                        callback.onResult(AuthResult.error(t.getMessage()));
                    }
                });
    }

    // ── Channel scope ─────────────────────────────────────────────────────

    public void searchChannelMessages(String channelId, String q, int page, int size,
                                      RepositoryCallback<PagedResponse<SearchMessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelMessages(token, channelId, q, page, size)
                .enqueue(wrapPagedCallback(callback));
    }

    public void searchChannelMembers(String channelId, String q,
                                     RepositoryCallback<List<SearchMemberDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelMembers(token, channelId, q)
                .enqueue(wrapListCallback(callback));
    }

    public void searchChannelChannels(String channelId, String q,
                                      RepositoryCallback<List<SearchChannelDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelChannels(token, channelId, q)
                .enqueue(wrapListCallback(callback));
    }

    public void searchChannelMedia(String channelId,
                                   RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelMedia(token, channelId)
                .enqueue(wrapListCallback(callback));
    }

    public void searchChannelFiles(String channelId,
                                   RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelFiles(token, channelId)
                .enqueue(wrapListCallback(callback));
    }

    public void searchChannelPins(String channelId, String q,
                                  RepositoryCallback<List<SearchMessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchChannelPins(token, channelId, q)
                .enqueue(wrapListCallback(callback));
    }

    // ── Server scope ──────────────────────────────────────────────────────

    public void searchServerMessages(String serverId, String q, int page, int size,
                                     RepositoryCallback<PagedResponse<SearchMessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerMessages(token, serverId, q, page, size)
                .enqueue(wrapPagedCallback(callback));
    }

    public void searchServerMembers(String serverId, String q,
                                    RepositoryCallback<List<SearchMemberDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerMembers(token, serverId, q)
                .enqueue(wrapListCallback(callback));
    }

    public void searchServerChannels(String serverId, String q,
                                     RepositoryCallback<List<SearchChannelDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerChannels(token, serverId, q)
                .enqueue(wrapListCallback(callback));
    }

    public void searchServerMedia(String serverId,
                                  RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerMedia(token, serverId)
                .enqueue(wrapListCallback(callback));
    }

    public void searchServerFiles(String serverId,
                                  RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerFiles(token, serverId)
                .enqueue(wrapListCallback(callback));
    }

    public void searchServerPins(String serverId, String q,
                                 RepositoryCallback<List<SearchMessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchServerPins(token, serverId, q)
                .enqueue(wrapListCallback(callback));
    }

    // ── DM scope ──────────────────────────────────────────────────────────

    public void searchDmFriends(String q, RepositoryCallback<List<SearchMemberDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchDmFriends(token, q)
                .enqueue(wrapListCallback(callback));
    }

    public void searchDmMessages(String q, int page, int size,
                                 RepositoryCallback<PagedResponse<SearchMessageDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchDmMessages(token, q, page, size)
                .enqueue(wrapPagedCallback(callback));
    }

    public void searchDmMedia(RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchDmMedia(token)
                .enqueue(wrapListCallback(callback));
    }

    public void searchDmFiles(RepositoryCallback<List<SearchAttachmentDto>> callback) {
        String token = getToken();
        if (token == null) { callback.onResult(AuthResult.error("Not authenticated")); return; }

        apiService.searchDmFiles(token)
                .enqueue(wrapListCallback(callback));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private <T> Callback<ApiResponse<List<T>>> wrapListCallback(RepositoryCallback<List<T>> callback) {
        return new Callback<ApiResponse<List<T>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<T>>> call, Response<ApiResponse<List<T>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<T>>> call, Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        };
    }

    private <T> Callback<ApiResponse<PagedResponse<T>>> wrapPagedCallback(
            RepositoryCallback<PagedResponse<T>> callback) {
        return new Callback<ApiResponse<PagedResponse<T>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedResponse<T>>> call,
                                   Response<ApiResponse<PagedResponse<T>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedResponse<T>>> call, Throwable t) {
                callback.onResult(AuthResult.error(t.getMessage()));
            }
        };
    }
}
