package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.repository.SettingsRepository;

public class SettingsViewModel extends ViewModel {

    public interface SettingsUpdateCallback {
        void onSuccess();
        void onError(String message);
    }

    private final SettingsRepository repository;

    public SettingsViewModel(SettingsRepository repository) {
        this.repository = repository;
    }

    public LiveData<String> getLanguage(String authHeader) {
        return repository.getLanguage(authHeader);
    }

    public void updateLanguage(String authHeader, String locale) {
        repository.updateLanguage(authHeader, locale, null);
    }

    public void updateLanguage(String authHeader, String locale, SettingsUpdateCallback callback) {
        repository.updateLanguage(authHeader, locale, callback);
    }
}
