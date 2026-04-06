package com.example.hubble.viewmodel.server;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.ServerRepository;

public class CreateChannelViewModelFactory implements ViewModelProvider.Factory {

    private final ServerRepository serverRepository;
    private final String serverId;

    public CreateChannelViewModelFactory(ServerRepository serverRepository, String serverId) {
        this.serverRepository = serverRepository;
        this.serverId = serverId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new CreateChannelViewModel(serverRepository, serverId);
    }
}
