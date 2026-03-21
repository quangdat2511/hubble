package com.example.hubble.viewmodel.server;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.RepositoryCallback;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.model.server.ServerMemberItem;

import java.util.List;

public class ServerSettingsViewModel extends ViewModel {
    private MutableLiveData<AuthResult<List<ServerMemberItem>>> membersState = new MutableLiveData<>();
    private MutableLiveData<AuthResult<Void>> kickState = new MutableLiveData<>();
    private MutableLiveData<AuthResult<Void>> banState = new MutableLiveData<>();
    private MutableLiveData<AuthResult<Void>> transferOwnershipState = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ServerMemberRepository memberRepository;
    private String currentServerId;

    public ServerSettingsViewModel(ServerMemberRepository repository) {
        this.memberRepository = repository;
    }

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

    public void consumeKickState() {
        kickState.setValue(null);
    }

    public void consumeBanState() {
        banState.setValue(null);
    }

    public void consumeTransferOwnershipState() {
        transferOwnershipState.setValue(null);
    }

    public LiveData<AuthResult<List<ServerMemberItem>>> getMembersState() {
        return membersState;
    }

    public LiveData<AuthResult<Void>> getKickState() {
        return kickState;
    }

    public LiveData<AuthResult<Void>> getBanState() {
        return banState;
    }

    public LiveData<AuthResult<Void>> getTransferOwnershipState() {
        return transferOwnershipState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
