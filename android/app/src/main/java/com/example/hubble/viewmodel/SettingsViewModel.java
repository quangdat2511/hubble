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

    public SettingsViewModel(AuthRepository authRepository, PushConfigRepository pushConfigRepository) {
        this.authRepository = authRepository;
        this.pushConfigRepository = pushConfigRepository;
    }

    public void logout() {
        authRepository.logout();
    }

    public void loadPushConfig() {
        pushConfigRepository.getPushConfig(_pushConfigState::setValue);
    }

    public void updatePushConfig(boolean notificationEnabled, boolean notificationSound) {
        pushConfigRepository.updatePushConfig(notificationEnabled, notificationSound, _pushConfigSaveState::setValue);
    }

    public void resetPushConfigState() {
        _pushConfigState.setValue(null);
    }

    public void resetPushConfigSaveState() {
        _pushConfigSaveState.setValue(null);
    }
}
