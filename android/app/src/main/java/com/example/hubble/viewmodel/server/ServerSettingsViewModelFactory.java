package com.example.hubble.viewmodel.server;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.repository.ServerRepository;

public class ServerSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final ServerMemberRepository memberRepository;
    private final ServerRepository serverRepository;

    public ServerSettingsViewModelFactory(ServerMemberRepository memberRepository,
                                          ServerRepository serverRepository) {
        this.memberRepository = memberRepository;
        this.serverRepository = serverRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ServerSettingsViewModel.class)) {
            return (T) new ServerSettingsViewModel(memberRepository, serverRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
