package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.SessionDto;
import com.example.hubble.data.repository.SessionRepository;
import java.util.List;

public class SessionViewModel extends ViewModel {
    private final SessionRepository repository;

    private final MutableLiveData<AuthResult<List<SessionDto>>> _sessions = new MutableLiveData<>();
    public final LiveData<AuthResult<List<SessionDto>>> sessions = _sessions;

    private final MutableLiveData<AuthResult<String>> _revokeState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> revokeState = _revokeState;

    public SessionViewModel(SessionRepository repository) {
        this.repository = repository;
    }

    public void fetchSessions() {
        repository.getActiveSessions(_sessions::postValue);
    }

    public void revokeSession(String sessionId) {
        repository.revokeSession(sessionId, _revokeState::postValue);
    }

    public void resetRevokeState() {
        _revokeState.setValue(null);
    }
}