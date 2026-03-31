package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;


import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.ThemeManager;

public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository;
    private final MutableLiveData<AuthResult<String>> themeState = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<String>> themeUpdateState = new MutableLiveData<>();

    private Observer<AuthResult<String>> themeObserver;
    private LiveData<AuthResult<String>> themeSource;
    private Observer<AuthResult<String>> themeUpdateObserver;
    private LiveData<AuthResult<String>> themeUpdateSource;

    private String cachedTheme = ThemeManager.THEME_DARK;
    private String pendingPreviousTheme;
    private String pendingThemeErrorMessage;
    private boolean localOverrideSinceFetch;

    public SettingsViewModel(SettingsRepository repository) {
        this.repository = repository;
    }

    public LiveData<AuthResult<String>> getTheme(String authHeader) {
        detachThemeSource();
        localOverrideSinceFetch = false;
        themeSource = repository.getTheme(authHeader);
        themeObserver = result -> {
            themeState.postValue(result);
            if (result != null && !result.isLoading()) {
                detachThemeSource();
            }
        };
        themeSource.observeForever(themeObserver);
        return themeState;
    }

    public LiveData<AuthResult<String>> updateTheme(String authHeader, String theme, String previousTheme) {
        detachThemeUpdateSource();
        pendingPreviousTheme = ThemeManager.normalizeTheme(previousTheme);
        cachedTheme = ThemeManager.normalizeTheme(theme);
        localOverrideSinceFetch = true;

        themeUpdateSource = repository.updateTheme(authHeader, theme);
        themeUpdateObserver = result -> {
            themeUpdateState.postValue(result);
            if (result != null && !result.isLoading()) {
                detachThemeUpdateSource();
            }
        };
        themeUpdateSource.observeForever(themeUpdateObserver);
        return themeUpdateState;
    }

    public LiveData<AuthResult<String>> getThemeState() {
        return themeState;
    }

    public LiveData<AuthResult<String>> getThemeUpdateState() {
        return themeUpdateState;
    }

    public void setCachedTheme(String theme) {
        cachedTheme = ThemeManager.normalizeTheme(theme);
    }

    public String getCachedTheme() {
        return cachedTheme;
    }

    public void markLocalOverride() {
        localOverrideSinceFetch = true;
    }

    public boolean hasLocalOverrideSinceFetch() {
        return localOverrideSinceFetch;
    }

    public String getPendingPreviousTheme() {
        return pendingPreviousTheme;
    }

    public void clearPendingPreviousTheme() {
        pendingPreviousTheme = null;
    }

    public void setPendingThemeErrorMessage(String pendingThemeErrorMessage) {
        this.pendingThemeErrorMessage = pendingThemeErrorMessage;
    }

    public String consumePendingThemeErrorMessage() {
        String errorMessage = pendingThemeErrorMessage;
        pendingThemeErrorMessage = null;
        return errorMessage;
    }

    public boolean isThemeUpdateInProgress() {
        AuthResult<String> result = themeUpdateState.getValue();
        return result != null && result.isLoading();
    }

    public void clearThemeState() {
        themeState.setValue(null);
    }

    public void clearThemeUpdateState() {
        themeUpdateState.setValue(null);
    }

    @Override
    protected void onCleared() {
        detachThemeSource();
        detachThemeUpdateSource();
        super.onCleared();
    }

    private void detachThemeSource() {
        if (themeSource != null && themeObserver != null) {
            themeSource.removeObserver(themeObserver);
        }
        themeSource = null;
        themeObserver = null;
    }

    private void detachThemeUpdateSource() {
        if (themeUpdateSource != null && themeUpdateObserver != null) {
            themeUpdateSource.removeObserver(themeUpdateObserver);
        }
        themeUpdateSource = null;
        themeUpdateObserver = null;
    }
}
