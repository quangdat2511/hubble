package com.example.hubble.viewmodel.server;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.ServerMemberRepository;

public class ServerSettingsViewModelFactory implements ViewModelProvider.Factory {
    private ServerMemberRepository repository;

    public ServerSettingsViewModelFactory(ServerMemberRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ServerSettingsViewModel.class)) {
            return (T) new ServerSettingsViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
