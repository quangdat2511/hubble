package com.example.hubble.viewmodel.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;

@SuppressWarnings("unused")
public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final DmRepository dmRepository;
    private final ServerRepository serverRepository;

    public MainViewModelFactory(DmRepository dmRepository, ServerRepository serverRepository) {
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(dmRepository, serverRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}


