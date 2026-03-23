package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.repository.SettingsRepository;

public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository;

    public SettingsViewModel(SettingsRepository repository) {
        this.repository = repository;
    }

    public LiveData<String> getTheme(String authHeader) {
        return repository.getTheme(authHeader);
    }

    public void updateTheme(String authHeader, String theme) {
        repository.updateTheme(authHeader, theme);
    }
}