package com.example.hubble.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.repository.ServerRepository;

public class CreateServerViewModel extends ViewModel {

    private final ServerRepository repository;

    private final MutableLiveData<String> _serverName = new MutableLiveData<>("");
    private final MutableLiveData<String> _serverType = new MutableLiveData<>("");
    private final MutableLiveData<Uri> _iconUri = new MutableLiveData<>();
    private final MutableLiveData<AuthResult<ServerItem>> _createState = new MutableLiveData<>();

    public final LiveData<String> serverName = _serverName;
    public final LiveData<String> serverType = _serverType;
    public final LiveData<Uri> iconUri = _iconUri;
    public final LiveData<AuthResult<ServerItem>> createState = _createState;

    public CreateServerViewModel(ServerRepository repository) {
        this.repository = repository;
    }

    public void setServerName(String name) { _serverName.setValue(name); }
    public void setServerType(String type) { _serverType.setValue(type); }
    public void setIconUri(Uri uri) { _iconUri.setValue(uri); }

    public void createServer() {
        String name = _serverName.getValue();
        String type = _serverType.getValue();
        repository.createServer(name, type, result -> _createState.postValue(result));
    }

    public void resetCreateState() { _createState.setValue(null); }
}
