package com.example.hubble.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.search.PagedResponse;
import com.example.hubble.data.model.search.SearchChannelDto;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.data.model.search.SearchMessageDto;
import com.example.hubble.data.repository.SearchRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {

    public enum ScopeType { CHANNEL, SERVER, DM }

    private SearchRepository repository;

    private final MutableLiveData<String> _currentQuery = new MutableLiveData<>("");
    public final LiveData<String> currentQuery = _currentQuery;

    private final MutableLiveData<ScopeType> _scopeType = new MutableLiveData<>();
    public final LiveData<ScopeType> scopeType = _scopeType;

    // Messages (paginated)
    private final MutableLiveData<AuthResult<List<SearchMessageDto>>> _messagesResult = new MutableLiveData<>();
    public final LiveData<AuthResult<List<SearchMessageDto>>> messagesResult = _messagesResult;
    private boolean messagesLastPage = false;
    private int messagesPage = 0;
    private String lastMessageQuery = "";

    // Members
    private final MutableLiveData<AuthResult<List<SearchMemberDto>>> _membersResult = new MutableLiveData<>();
    public final LiveData<AuthResult<List<SearchMemberDto>>> membersResult = _membersResult;

    // Channels (server scope only)
    private final MutableLiveData<AuthResult<List<SearchChannelDto>>> _channelsResult = new MutableLiveData<>();
    public final LiveData<AuthResult<List<SearchChannelDto>>> channelsResult = _channelsResult;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private String scopeId;

    public void init(Context context, ScopeType scope, String scopeId) {
        this.repository = new SearchRepository(context);
        this._scopeType.setValue(scope);
        this.scopeId = scopeId;
        // Load tabs that can be shown immediately when query is empty.
        loadNonQueryTabs(scope, scopeId);
    }

    /** Load tabs that don't require a search query. */
    private void loadNonQueryTabs(ScopeType scope, String scopeId) {
        switch (scope) {
            case CHANNEL:
                repository.searchChannelMembers(scopeId, "", result -> _membersResult.postValue(result));
                repository.searchChannelChannels(scopeId, "", result -> _channelsResult.postValue(result));
                break;
            case SERVER:
                repository.searchServerMembers(scopeId, "", result -> _membersResult.postValue(result));
                repository.searchServerChannels(scopeId, "", result -> _channelsResult.postValue(result));
                break;
            case DM:
                repository.searchDmFriends("", result -> _membersResult.postValue(result));
                break;
        }
    }

    /** Debounced search triggered by text change. */
    public void onQueryChanged(String query) {
        _currentQuery.setValue(query);
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        debounceRunnable = () -> searchAll(query);
        debounceHandler.postDelayed(debounceRunnable, 300);
    }

    public void searchAll(String query) {
        ScopeType scope = _scopeType.getValue();
        if (scope == null) return;
        if (scope != ScopeType.DM && scopeId == null) return;

        String trimmed = query == null ? "" : query.trim();
        lastMessageQuery = trimmed;
        messagesPage = 0;

        // Reload non-query tabs with filter
        switch (scope) {
            case CHANNEL:
                if (!trimmed.isEmpty()) {
                    _messagesResult.setValue(AuthResult.loading());
                    repository.searchChannelMessages(scopeId, trimmed, 0, 20, result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            messagesLastPage = result.getData().isLast();
                            _messagesResult.postValue(AuthResult.success(
                                    safeList(result.getData().getContent())));
                        } else {
                            _messagesResult.postValue(result.isError()
                                    ? AuthResult.error(result.getMessage())
                                    : AuthResult.success(new ArrayList<>()));
                        }
                    });
                }
                repository.searchChannelMembers(scopeId, trimmed, result -> _membersResult.postValue(result));
                repository.searchChannelChannels(scopeId, trimmed, result -> _channelsResult.postValue(result));
                break;

            case SERVER:
                if (!trimmed.isEmpty()) {
                    _messagesResult.setValue(AuthResult.loading());
                    repository.searchServerMessages(scopeId, trimmed, 0, 20, result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            messagesLastPage = result.getData().isLast();
                            _messagesResult.postValue(AuthResult.success(
                                    safeList(result.getData().getContent())));
                        } else {
                            _messagesResult.postValue(result.isError()
                                    ? AuthResult.error(result.getMessage())
                                    : AuthResult.success(new ArrayList<>()));
                        }
                    });
                }
                repository.searchServerMembers(scopeId, trimmed, result -> _membersResult.postValue(result));
                repository.searchServerChannels(scopeId, trimmed, result -> _channelsResult.postValue(result));
                break;

            case DM:
                if (!trimmed.isEmpty()) {
                    _messagesResult.setValue(AuthResult.loading());
                    repository.searchDmMessages(trimmed, 0, 20, result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            messagesLastPage = result.getData().isLast();
                            _messagesResult.postValue(AuthResult.success(
                                    safeList(result.getData().getContent())));
                        } else {
                            _messagesResult.postValue(result.isError()
                                    ? AuthResult.error(result.getMessage())
                                    : AuthResult.success(new ArrayList<>()));
                        }
                    });
                } else {
                    messagesLastPage = true;
                    _messagesResult.postValue(AuthResult.success(new ArrayList<>()));
                }
                repository.searchDmFriends(trimmed, result -> _membersResult.postValue(result));
                break;
        }
    }

    /** Load next page of message results. */
    public void loadMoreMessages() {
        if (messagesLastPage) return;
        ScopeType scope = _scopeType.getValue();
        if (scope == null || lastMessageQuery.isEmpty()) return;
        if ((scope == ScopeType.CHANNEL || scope == ScopeType.SERVER) && scopeId == null) return;

        int nextPage = messagesPage + 1;
        switch (scope) {
            case CHANNEL:
                repository.searchChannelMessages(scopeId, lastMessageQuery, nextPage, 20, result -> {
                    appendMessages(result, nextPage);
                });
                break;
            case SERVER:
                repository.searchServerMessages(scopeId, lastMessageQuery, nextPage, 20, result -> {
                    appendMessages(result, nextPage);
                });
                break;
            case DM:
                repository.searchDmMessages(lastMessageQuery, nextPage, 20, result -> {
                    appendMessages(result, nextPage);
                });
                break;
        }
    }

    private void appendMessages(AuthResult<PagedResponse<SearchMessageDto>> result, int page) {
        if (result.isSuccess() && result.getData() != null) {
            messagesPage = page;
            messagesLastPage = result.getData().isLast();
            List<SearchMessageDto> existing = new ArrayList<>();
            AuthResult<List<SearchMessageDto>> current = _messagesResult.getValue();
            if (current != null && current.isSuccess() && current.getData() != null) {
                existing.addAll(current.getData());
            }
            existing.addAll(safeList(result.getData().getContent()));
            _messagesResult.postValue(AuthResult.success(existing));
        }
    }

    public boolean isMessagesLastPage() { return messagesLastPage; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }
}
