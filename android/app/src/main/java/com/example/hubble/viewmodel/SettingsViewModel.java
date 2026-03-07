package com.example.hubble.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.hubble.data.repository.AuthRepository;

public class SettingsViewModel extends ViewModel {

    private final AuthRepository repository;

    public SettingsViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public void logout() {
        repository.logout();
    }
}
