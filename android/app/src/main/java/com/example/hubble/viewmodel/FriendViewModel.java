package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.repository.FriendRepository;
import java.util.List;

public class FriendViewModel extends ViewModel {
    private final FriendRepository repository;

    private final MutableLiveData<AuthResult<List<FriendUserDto>>> _searchResults = new MutableLiveData<>();
    public final LiveData<AuthResult<List<FriendUserDto>>> searchResults = _searchResults;

    private final MutableLiveData<AuthResult<FriendRequestResponse>> _sendRequestState = new MutableLiveData<>();
    public final LiveData<AuthResult<FriendRequestResponse>> sendRequestState = _sendRequestState;

    private final MutableLiveData<AuthResult<List<FriendRequestResponse>>> _incomingRequests = new MutableLiveData<>();
    public final LiveData<AuthResult<List<FriendRequestResponse>>> incomingRequests = _incomingRequests;

    private final MutableLiveData<AuthResult<String>> _actionState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> actionState = _actionState;

    private final MutableLiveData<AuthResult<List<FriendRequestResponse>>> _outgoingRequests = new MutableLiveData<>();
    public final LiveData<AuthResult<List<FriendRequestResponse>>> outgoingRequests = _outgoingRequests;

    private final MutableLiveData<AuthResult<List<FriendUserDto>>> _blockedUsers = new MutableLiveData<>();
    public final LiveData<AuthResult<List<FriendUserDto>>> blockedUsers = _blockedUsers;

    public FriendViewModel(FriendRepository repository) {
        this.repository = repository;
    }

    public void fetchOutgoingRequests() {
        _outgoingRequests.setValue(AuthResult.loading());
        repository.getOutgoingRequests(_outgoingRequests::postValue);
    }

    public void sendRequestById(String userId) {
        _sendRequestState.setValue(AuthResult.loading());
        repository.sendRequestById(userId, _sendRequestState::postValue);
    }

    public void cancelOutgoingRequest(String requestId) {
        _actionState.setValue(AuthResult.loading());
        repository.declineRequest(requestId, _actionState::postValue);
    }

    public void fetchBlockedUsers() {
        _blockedUsers.setValue(AuthResult.loading());
        repository.getBlockedUsers(_blockedUsers::postValue);
    }

    public void blockUser(String userId) {
        _actionState.setValue(AuthResult.loading());
        repository.blockUser(userId, _actionState::postValue);
    }

    public void unblockUser(String userId) {
        _actionState.setValue(AuthResult.loading());
        repository.unblockUser(userId, _actionState::postValue);
    }

    public void searchUsers(String query) {
        _searchResults.setValue(AuthResult.loading());
        repository.searchUsers(query, _searchResults::postValue);
    }

    public void sendRequest(String username) {
        _sendRequestState.setValue(AuthResult.loading());
        repository.sendRequestByUsername(username, _sendRequestState::postValue);
    }

    public void fetchIncomingRequests() {
        _incomingRequests.setValue(AuthResult.loading());
        repository.getIncomingRequests(_incomingRequests::postValue);
    }

    public void acceptRequest(String requestId) {
        _actionState.setValue(AuthResult.loading());
        repository.acceptRequest(requestId, _actionState::postValue);
    }

    public void declineRequest(String requestId) {
        _actionState.setValue(AuthResult.loading());
        repository.declineRequest(requestId, _actionState::postValue);
    }

    public void resetActionState() {
        _actionState.setValue(null);
    }

    public void resetSendState() {
        _sendRequestState.setValue(null);
    }
}