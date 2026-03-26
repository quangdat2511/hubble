package com.example.hubble.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;

public class SettingsViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository authRepository;
    private final PushConfigRepository pushConfigRepository;

    public SettingsViewModelFactory(AuthRepository authRepository,
                                    PushConfigRepository pushConfigRepository) {
        this.authRepository = authRepository;
        this.pushConfigRepository = pushConfigRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(authRepository, pushConfigRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
