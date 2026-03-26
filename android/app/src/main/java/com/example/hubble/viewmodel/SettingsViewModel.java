package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
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

    private final MutableLiveData<AuthResult<PushConfigResponse>> pushConfigStateInternal = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigState = pushConfigStateInternal;

    private final MutableLiveData<AuthResult<PushConfigResponse>> pushConfigSaveStateInternal = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigSaveState = pushConfigSaveStateInternal;

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

    public SettingsViewModel(AuthRepository authRepository) {
        this(authRepository, null, null);
    }

    public SettingsViewModel(SettingsRepository settingsRepository) {
        this(null, settingsRepository, null);
    }

    public SettingsViewModel(AuthRepository authRepository, PushConfigRepository pushConfigRepository) {
        this(authRepository, null, pushConfigRepository);
    }

    public SettingsViewModel(AuthRepository authRepository, SettingsRepository settingsRepository) {
        this(authRepository, settingsRepository, null);
    }

    public SettingsViewModel(AuthRepository authRepository,
                             SettingsRepository settingsRepository,
                             PushConfigRepository pushConfigRepository) {
        this.authRepository = authRepository;
        this.settingsRepository = settingsRepository;
        this.pushConfigRepository = pushConfigRepository;
    }

    public void logout() {
        if (authRepository != null) {
            authRepository.logout();
        }
    }

    public LiveData<String> getLanguage(String authHeader) {
        if (settingsRepository == null) {
            throw new IllegalStateException("SettingsRepository is required for language operations");
        }
        return settingsRepository.getLanguage(authHeader);
    }

    public void updateLanguage(String authHeader, String locale) {
        updateLanguage(authHeader, locale, null);
    }

    public void updateLanguage(String authHeader, String locale, SettingsUpdateCallback callback) {
        if (settingsRepository == null) {
            throw new IllegalStateException("SettingsRepository is required for language operations");
        }
        settingsRepository.updateLanguage(authHeader, locale, callback);
    }

    public void loadPushConfig() {
        if (pushConfigRepository == null) {
            throw new IllegalStateException("PushConfigRepository is required for push settings");
        }
        pushConfigRepository.getPushConfig(pushConfigStateInternal::setValue);
    }

    public void updatePushConfig(boolean notificationEnabled, boolean notificationSound) {
        if (pushConfigRepository == null) {
            throw new IllegalStateException("PushConfigRepository is required for push settings");
        }
        pushConfigRepository.updatePushConfig(
                notificationEnabled,
                notificationSound,
                pushConfigSaveStateInternal::setValue
        );
    }

    public void resetPushConfigState() {
        pushConfigStateInternal.setValue(null);
    }

    public void resetPushConfigSaveState() {
        pushConfigSaveStateInternal.setValue(null);
    }

    public LiveData<AuthResult<String>> getTheme(String authHeader) {
        if (settingsRepository == null) {
            throw new IllegalStateException("SettingsRepository is required for theme operations");
        }

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
        if (settingsRepository == null) {
            throw new IllegalStateException("SettingsRepository is required for theme operations");
        }

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
