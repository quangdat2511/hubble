package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.ThemeManager;

public class SettingsViewModel extends ViewModel {

    public interface SettingsUpdateCallback {
        void onSuccess();
        void onError(String message);
    }

    private final AuthRepository authRepository;
    private final SettingsRepository settingsRepository;
    private final PushConfigRepository pushConfigRepository;

    private final MutableLiveData<AuthResult<PushConfigResponse>> pushConfigStateMutable = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigState = pushConfigStateMutable;

    private final MutableLiveData<AuthResult<PushConfigResponse>> pushConfigSaveStateMutable = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigSaveState = pushConfigSaveStateMutable;

    private final MutableLiveData<PushConfigResponse> currentPushConfigMutable = new MutableLiveData<>();
    public final LiveData<PushConfigResponse> currentPushConfig = currentPushConfigMutable;

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

    public SettingsViewModel(AuthRepository authRepository,
                             SettingsRepository settingsRepository,
                             PushConfigRepository pushConfigRepository) {
        this.authRepository = authRepository;
        this.settingsRepository = settingsRepository;
        this.pushConfigRepository = pushConfigRepository;
    }

    public void logout() {
        authRepository.logout();
    }

    public LiveData<AuthResult<String>> getTheme(String authHeader) {
        detachThemeSource();
        localOverrideSinceFetch = false;
        themeSource = settingsRepository.getTheme(authHeader);
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

        themeUpdateSource = settingsRepository.updateTheme(authHeader, theme);
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

    public LiveData<String> getLanguage(String authHeader) {
        return settingsRepository.getLanguage(authHeader);
    }

    public void updateLanguage(String authHeader, String locale) {
        settingsRepository.updateLanguage(authHeader, locale, null);
    }

    public void updateLanguage(String authHeader, String locale, SettingsUpdateCallback callback) {
        settingsRepository.updateLanguage(authHeader, locale, callback);
    }

    public void loadPushConfig() {
        pushConfigRepository.getPushConfig(result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                currentPushConfigMutable.setValue(result.getData());
            }
            pushConfigStateMutable.setValue(result);
        });
    }

    public void updatePushConfig(boolean notificationEnabled, boolean notificationSound) {
        pushConfigRepository.updatePushConfig(notificationEnabled, notificationSound, result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                currentPushConfigMutable.setValue(result.getData());
            }
            pushConfigSaveStateMutable.setValue(result);
        });
    }

    public PushConfigResponse getCurrentPushConfigValue() {
        return currentPushConfigMutable.getValue();
    }

    public void resetPushConfigState() {
        pushConfigStateMutable.setValue(null);
    }

    public void resetPushConfigSaveState() {
        pushConfigSaveStateMutable.setValue(null);
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
