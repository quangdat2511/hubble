package com.example.hubble.viewmodel.server;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.CreateChannelRequest;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.ServerMemberResponse;
import com.example.hubble.data.repository.ServerRepository;

import java.util.ArrayList;
import java.util.List;

public class CreateChannelViewModel extends ViewModel {

    private final ServerRepository serverRepository;
    private final String serverId;
    private String parentId;

    private final MutableLiveData<String> _channelName = new MutableLiveData<>("kênh-mới");
    private final MutableLiveData<String> _channelType = new MutableLiveData<>("TEXT");
    private final MutableLiveData<Boolean> _isPrivate = new MutableLiveData<>(false);
    private final MutableLiveData<AuthResult<ChannelDto>> _createState = new MutableLiveData<>();

    // Selected members and roles for private channel
    private final List<String> selectedMemberIds = new ArrayList<>();
    private final List<String> selectedRoleIds = new ArrayList<>();

    public final LiveData<String> channelName = _channelName;
    public final LiveData<String> channelType = _channelType;
    public final LiveData<Boolean> isPrivate = _isPrivate;
    public final LiveData<AuthResult<ChannelDto>> createState = _createState;

    public CreateChannelViewModel(ServerRepository serverRepository, String serverId) {
        this.serverRepository = serverRepository;
        this.serverId = serverId;
    }

    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getParentId() { return parentId; }

    public void setChannelName(String name) { _channelName.setValue(name); }
    public void setChannelType(String type) { _channelType.setValue(type); }
    public void setIsPrivate(boolean isPrivate) { _isPrivate.setValue(isPrivate); }

    public List<String> getSelectedMemberIds() { return selectedMemberIds; }
    public List<String> getSelectedRoleIds() { return selectedRoleIds; }

    public void toggleMember(String memberId) {
        if (selectedMemberIds.contains(memberId)) {
            selectedMemberIds.remove(memberId);
        } else {
            selectedMemberIds.add(memberId);
        }
    }

    public void toggleRole(String roleId) {
        if (selectedRoleIds.contains(roleId)) {
            selectedRoleIds.remove(roleId);
        } else {
            selectedRoleIds.add(roleId);
        }
    }

    public boolean hasSelections() {
        return !selectedMemberIds.isEmpty() || !selectedRoleIds.isEmpty();
    }

    public void createChannel() {
        String name = _channelName.getValue();
        String type = _channelType.getValue();
        Boolean priv = _isPrivate.getValue();

        CreateChannelRequest request = new CreateChannelRequest(
                name, type, parentId,
                priv != null && priv,
                priv != null && priv ? new ArrayList<>(selectedMemberIds) : null,
                priv != null && priv ? new ArrayList<>(selectedRoleIds) : null
        );

        serverRepository.createChannel(serverId, request, result -> _createState.postValue(result));
    }

    public void resetCreateState() { _createState.setValue(null); }
}
