package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;

public class SettingsViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final PushConfigRepository pushConfigRepository;

    private final MutableLiveData<AuthResult<PushConfigResponse>> _pushConfigState = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigState = _pushConfigState;

    private final MutableLiveData<AuthResult<PushConfigResponse>> _pushConfigSaveState = new MutableLiveData<>();
    public final LiveData<AuthResult<PushConfigResponse>> pushConfigSaveState = _pushConfigSaveState;

    private final MutableLiveData<PushConfigResponse> _currentPushConfig = new MutableLiveData<>();
    public final LiveData<PushConfigResponse> currentPushConfig = _currentPushConfig;

    public SettingsViewModel(AuthRepository authRepository, PushConfigRepository pushConfigRepository) {
        this.authRepository = authRepository;
        this.pushConfigRepository = pushConfigRepository;
    }

    public void logout() {
        authRepository.logout();
    }

    public void loadPushConfig() {
        pushConfigRepository.getPushConfig(result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                _currentPushConfig.setValue(result.getData());
            }
            _pushConfigState.setValue(result);
        });
    }

    public void updatePushConfig(boolean notificationEnabled, boolean notificationSound) {
        pushConfigRepository.updatePushConfig(notificationEnabled, notificationSound, result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                _currentPushConfig.setValue(result.getData());
            }
            _pushConfigSaveState.setValue(result);
        });
    }

    public PushConfigResponse getCurrentPushConfigValue() {
        return _currentPushConfig.getValue();
    }

    public void resetPushConfigState() {
        _pushConfigState.setValue(null);
    }

    public void resetPushConfigSaveState() {
        _pushConfigSaveState.setValue(null);
    }
}
