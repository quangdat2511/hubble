package com.example.hubble.viewmodel.server;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.ServerInviteRepository;

public class ServerInviteViewModelFactory implements ViewModelProvider.Factory {

    private final ServerInviteRepository repository;

    public ServerInviteViewModelFactory(ServerInviteRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ServerInviteViewModel.class)) {
            //noinspection unchecked
            return (T) new ServerInviteViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}

