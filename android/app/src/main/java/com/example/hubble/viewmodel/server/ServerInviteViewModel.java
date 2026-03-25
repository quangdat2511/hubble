package com.example.hubble.viewmodel.server;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.data.repository.ServerInviteRepository;

import java.util.List;

public class ServerInviteViewModel extends ViewModel {

    private final MutableLiveData<AuthResult<List<ServerInviteResponse>>> serverInvitesState =
            new MutableLiveData<>();
    private final MutableLiveData<AuthResult<List<ServerInviteResponse>>> myInvitesState =
            new MutableLiveData<>();
    private final MutableLiveData<AuthResult<ServerInviteResponse>> createInviteState =
            new MutableLiveData<>();
    private final MutableLiveData<AuthResult<ServerInviteResponse>> respondState =
            new MutableLiveData<>();
    // Fires true only when an invite is accepted — consumers refresh their server list
    private final MutableLiveData<Boolean> acceptSuccessEvent = new MutableLiveData<>();

    private final ServerInviteRepository repository;
    private String currentServerId;

    public ServerInviteViewModel(ServerInviteRepository repository) {
        this.repository = repository;
    }

    // ── Server invite management ──────────────────────────────────────

    public void loadServerInvites(String serverId) {
        this.currentServerId = serverId;
        repository.getServerInvites(serverId, result -> serverInvitesState.setValue(result));
    }

    public void createInvite(String inviteeUsername) {
        if (currentServerId == null) return;
        repository.createInvite(currentServerId, inviteeUsername, result -> {
            createInviteState.setValue(result);
            if (result.isSuccess()) {
                // Refresh the list after successful creation
                loadServerInvites(currentServerId);
            }
        });
    }

    // ── My invites (received by current user) ────────────────────────

    public void loadMyInvites() {
        repository.getMyInvites(result -> myInvitesState.setValue(result));
    }

    public void acceptInvite(String inviteId) {
        repository.acceptInvite(inviteId, result -> {
            respondState.setValue(result);
            if (result.isSuccess()) {
                acceptSuccessEvent.setValue(true); // signal server list reload
                loadMyInvites();
            }
        });
    }

    public void declineInvite(String inviteId) {
        repository.declineInvite(inviteId, result -> {
            respondState.setValue(result);
            if (result.isSuccess()) {
                loadMyInvites();
            }
        });
    }

    // ── Consume one-shot events ───────────────────────────────────────

    public void consumeCreateInviteState() {
        createInviteState.setValue(null);
    }

    public void consumeRespondState() {
        respondState.setValue(null);
    }

    public void consumeAcceptSuccessEvent() {
        acceptSuccessEvent.setValue(null);
    }

    // ── LiveData getters ──────────────────────────────────────────────

    public LiveData<AuthResult<List<ServerInviteResponse>>> getServerInvitesState() {
        return serverInvitesState;
    }

    public LiveData<AuthResult<List<ServerInviteResponse>>> getMyInvitesState() {
        return myInvitesState;
    }

    public LiveData<AuthResult<ServerInviteResponse>> getCreateInviteState() {
        return createInviteState;
    }

    public LiveData<AuthResult<ServerInviteResponse>> getRespondState() {
        return respondState;
    }

    public LiveData<Boolean> getAcceptSuccessEvent() {
        return acceptSuccessEvent;
    }
}

