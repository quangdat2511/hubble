package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.data.model.server.PermissionResponse;
import com.example.hubble.data.model.server.RoleDetailResponse;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.repository.RoleRepository;

import java.util.List;
import java.util.Map;

public class RolesViewModel extends ViewModel {

    private final RoleRepository repository;

    private final MutableLiveData<AuthResult<List<RoleResponse>>> _roles = new MutableLiveData<>();
    public final LiveData<AuthResult<List<RoleResponse>>> roles = _roles;

    private final MutableLiveData<AuthResult<RoleDetailResponse>> _roleDetail = new MutableLiveData<>();
    public final LiveData<AuthResult<RoleDetailResponse>> roleDetail = _roleDetail;

    private final MutableLiveData<AuthResult<RoleResponse>> _createResult = new MutableLiveData<>();
    public final LiveData<AuthResult<RoleResponse>> createResult = _createResult;

    private final MutableLiveData<AuthResult<RoleResponse>> _updateResult = new MutableLiveData<>();
    public final LiveData<AuthResult<RoleResponse>> updateResult = _updateResult;

    private final MutableLiveData<AuthResult<Void>> _deleteResult = new MutableLiveData<>();
    public final LiveData<AuthResult<Void>> deleteResult = _deleteResult;

    private final MutableLiveData<AuthResult<List<PermissionResponse>>> _permissions = new MutableLiveData<>();
    public final LiveData<AuthResult<List<PermissionResponse>>> permissions = _permissions;

    private final MutableLiveData<AuthResult<List<MemberBriefResponse>>> _members = new MutableLiveData<>();
    public final LiveData<AuthResult<List<MemberBriefResponse>>> members = _members;

    public RolesViewModel(RoleRepository repository) {
        this.repository = repository;
    }

    public void loadRoles(String serverId) {
        repository.getRoles(serverId, result -> _roles.postValue(result));
    }

    public void loadRoleDetail(String serverId, String roleId) {
        repository.getRoleDetail(serverId, roleId, result -> _roleDetail.postValue(result));
    }

    public void createRole(String serverId, String name, Integer color, String preset,
                           List<String> memberIds) {
        repository.createRole(serverId, name, color, preset, memberIds,
                result -> _createResult.postValue(result));
    }

    public void updateRole(String serverId, String roleId, Map<String, Object> fields) {
        repository.updateRole(serverId, roleId, fields, result -> _updateResult.postValue(result));
    }

    public void deleteRole(String serverId, String roleId) {
        repository.deleteRole(serverId, roleId, result -> _deleteResult.postValue(result));
    }

    public void loadPermissions(String serverId, String roleId) {
        repository.getPermissions(serverId, roleId, result -> _permissions.postValue(result));
    }

    public void updatePermissions(String serverId, String roleId, List<String> grantedPermissions) {
        repository.updatePermissions(serverId, roleId, grantedPermissions,
                result -> _permissions.postValue(result));
    }

    public void loadMembers(String serverId, String roleId) {
        repository.getMembers(serverId, roleId, result -> _members.postValue(result));
    }

    public void assignMembers(String serverId, String roleId, List<String> memberIds) {
        repository.assignMembers(serverId, roleId, memberIds, result -> _members.postValue(result));
    }

    public void removeMember(String serverId, String roleId, String memberId) {
        repository.removeMember(serverId, roleId, memberId, result -> {
            // After removing, we don't post to _members directly — caller should reload
        });
    }

    public void resetCreateResult() { _createResult.setValue(null); }
    public void resetDeleteResult() { _deleteResult.setValue(null); }
    public void resetRoles() { _roles.setValue(null); }
    public void resetPermissions() { _permissions.setValue(null); }
    public void resetMembers() { _members.setValue(null); }
}
