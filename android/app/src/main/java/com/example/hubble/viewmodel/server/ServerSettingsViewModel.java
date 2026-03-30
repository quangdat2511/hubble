package com.example.hubble.viewmodel.server;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.repository.RepositoryCallback;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.repository.ServerRepository;

import java.util.List;

public class ServerSettingsViewModel extends ViewModel {

    private final MutableLiveData<AuthResult<List<ServerMemberItem>>> membersState = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<Void>> kickState = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<Void>> banState = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<Void>> transferOwnershipState = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<ServerItem>> _iconState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final ServerMemberRepository memberRepository;
    private final ServerRepository serverRepository;
    private String currentServerId;

    public ServerSettingsViewModel(ServerMemberRepository memberRepository,
                                   ServerRepository serverRepository) {
        this.memberRepository = memberRepository;
        this.serverRepository = serverRepository;
    }

    // ── Members ───────────────────────────────────────────────────────────

    public void loadMembers(String serverId) {
        this.currentServerId = serverId;
        memberRepository.getServerMembers(serverId, result -> {
            membersState.setValue(result);
            if (result.getStatus() == AuthResult.Status.ERROR) {
                errorMessage.setValue(result.getMessage());
            }
        });
    }

    public void kickMember(String memberId) {
        if (currentServerId == null) return;
        memberRepository.kickMember(currentServerId, memberId, result -> {
            kickState.setValue(result);
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                loadMembers(currentServerId);
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                errorMessage.setValue(result.getMessage());
            }
        });
    }

    public void banMember(String memberId) {
        if (currentServerId == null) return;
        memberRepository.banMember(currentServerId, memberId, result -> {
            banState.setValue(result);
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                loadMembers(currentServerId);
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                errorMessage.setValue(result.getMessage());
            }
        });
    }

    public void transferOwnership(String memberId) {
        if (currentServerId == null) return;
        memberRepository.transferOwnership(currentServerId, memberId, result -> {
            transferOwnershipState.setValue(result);
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                loadMembers(currentServerId);
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                errorMessage.setValue(result.getMessage());
            }
        });
    }

    // ── Icon management ───────────────────────────────────────────────────

    public LiveData<AuthResult<ServerItem>> getIconState() { return _iconState; }

    public void updateServerIcon(String serverId, Uri iconUri) {
        _iconState.setValue(AuthResult.loading());
        serverRepository.updateServerIcon(serverId, iconUri,
                result -> _iconState.postValue(result));
    }

    public void deleteServerIcon(String serverId) {
        _iconState.setValue(AuthResult.loading());
        serverRepository.deleteServerIcon(serverId,
                result -> _iconState.postValue(result));
    }

    public void consumeIconState() { _iconState.setValue(null); }

    // ── Consume helpers ───────────────────────────────────────────────────

    public void consumeKickState()             { kickState.setValue(null); }
    public void consumeBanState()              { banState.setValue(null); }
    public void consumeTransferOwnershipState(){ transferOwnershipState.setValue(null); }

    // ── Getters ───────────────────────────────────────────────────────────

    public LiveData<AuthResult<List<ServerMemberItem>>> getMembersState()       { return membersState; }
    public LiveData<AuthResult<Void>> getKickState()                            { return kickState; }
    public LiveData<AuthResult<Void>> getBanState()                             { return banState; }
    public LiveData<AuthResult<Void>> getTransferOwnershipState()               { return transferOwnershipState; }
    public LiveData<String> getErrorMessage()                                   { return errorMessage; }
}
