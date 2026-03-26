package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;

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
}
