package com.hubble.service;

import com.hubble.dto.request.*;
import com.hubble.dto.response.*;
import com.hubble.entity.MemberRole;
import com.hubble.entity.Role;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.entity.User;
import com.hubble.enums.Permission;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.RoleMapper;
import com.hubble.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {

    RoleRepository roleRepository;
    MemberRoleRepository memberRoleRepository;
    ChannelRoleRepository channelRoleRepository;
    ServerRepository serverRepository;
    ServerMemberRepository serverMemberRepository;
    UserRepository userRepository;
    RoleMapper roleMapper;
    SimpMessagingTemplate messagingTemplate;

    // ──────────────── preset bitmasks ────────────────
    static final long PRESET_MEMBER = Permission.buildBitmask(List.of(
            Permission.VIEW_CHANNELS, Permission.SEND_MESSAGES,
            Permission.ATTACH_FILES, Permission.INVITE_MEMBERS
    ));
    // ADMIN gets all permissions except HIDE_FROM_SEARCH (that one is opt-in per role)
    static final long PRESET_ADMIN = Permission.buildBitmask(List.of(
            Permission.VIEW_CHANNELS, Permission.MANAGE_CHANNELS,
            Permission.MANAGE_ROLES, Permission.MANAGE_SERVER,
            Permission.INVITE_MEMBERS, Permission.KICK_MEMBERS,
            Permission.SEND_MESSAGES, Permission.ATTACH_FILES
    ));

    // ──────────────── queries ────────────────

    public List<RoleResponse> getRoles(UUID serverId) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        return roleRepository.findByServerIdOrderByPositionDesc(serverId).stream()
                .map(role -> {
                    RoleResponse resp = roleMapper.toRoleResponse(role);
                    resp.setMemberCount(memberRoleRepository.countByRoleId(role.getId()));
                    return resp;
                })
                .toList();
    }

    public RoleDetailResponse getRoleDetail(UUID serverId, UUID roleId) {
        Role role = getRole(serverId, roleId);

        List<PermissionResponse> permDetails = Arrays.stream(Permission.values())
                .map(p -> PermissionResponse.builder()
                        .name(p.name())
                        .description(p.name())
                        .granted(Permission.hasPermission(role.getPermissions(), p))
                        .build())
                .toList();

        List<MemberBriefResponse> members = buildMemberList(roleId);

        return RoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .color(role.getColor())
                .permissions(role.getPermissions())
                .displaySeparately(role.getDisplaySeparately())
                .mentionable(role.getMentionable())
                .memberCount(members.size())
                .permissionDetails(permDetails)
                .members(members)
                .build();
    }

    // ──────────────── commands ────────────────

    @Transactional
    public RoleResponse createRole(UUID serverId, CreateRoleRequest request) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (roleRepository.existsByServerIdAndNameIgnoreCase(serverId, request.getName())) {
            throw new AppException(ErrorCode.ROLE_NAME_DUPLICATED);
        }

        int nextPosition = roleRepository.countByServerId(serverId);
        long permsBitmask = resolvePreset(request.getPreset());

        Role role = Role.builder()
                .serverId(serverId)
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : 0)
                .permissions(permsBitmask)
                .position((short) nextPosition)
                .isDefault(false)
                .displaySeparately(false)
                .mentionable(false)
                .build();

        role = roleRepository.save(role);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            assignMembersInternal(role.getId(), serverId, request.getMemberIds());
        }

        RoleResponse resp = roleMapper.toRoleResponse(role);
        resp.setMemberCount(memberRoleRepository.countByRoleId(role.getId()));
        return resp;
    }

    @Transactional
    public RoleResponse updateRole(UUID serverId, UUID roleId, UpdateRoleRequest request) {
        Role role = getRole(serverId, roleId);

        if (request.getName() != null) {
            if (Boolean.TRUE.equals(role.getIsDefault())) {
                throw new AppException(ErrorCode.CANNOT_MODIFY_DEFAULT_ROLE_NAME);
            }
            if (!role.getName().equalsIgnoreCase(request.getName())
                    && roleRepository.existsByServerIdAndNameIgnoreCase(serverId, request.getName())) {
                throw new AppException(ErrorCode.ROLE_NAME_DUPLICATED);
            }
            role.setName(request.getName());
        }
        if (request.getColor() != null) role.setColor(request.getColor());
        if (request.getDisplaySeparately() != null) role.setDisplaySeparately(request.getDisplaySeparately());
        if (request.getMentionable() != null) role.setMentionable(request.getMentionable());

        role = roleRepository.save(role);
        RoleResponse resp = roleMapper.toRoleResponse(role);
        resp.setMemberCount(memberRoleRepository.countByRoleId(role.getId()));
        return resp;
    }

    @Transactional
    public void deleteRole(UUID serverId, UUID roleId) {
        Role role = getRole(serverId, roleId);
        if (Boolean.TRUE.equals(role.getIsDefault())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_DEFAULT_ROLE);
        }
        channelRoleRepository.deleteByRoleId(roleId);
        memberRoleRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
    }

    @Transactional
    public List<RoleResponse> reorderRoles(UUID serverId, ReorderRolesRequest request) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        Map<UUID, Integer> posMap = request.getPositions().stream()
                .collect(Collectors.toMap(
                        ReorderRolesRequest.RolePositionEntry::getRoleId,
                        ReorderRolesRequest.RolePositionEntry::getPosition));

        List<Role> roles = roleRepository.findByServerIdOrderByPositionDesc(serverId);
        for (Role role : roles) {
            Integer newPos = posMap.get(role.getId());
            if (newPos != null) {
                role.setPosition(newPos.shortValue());
            }
        }
        roleRepository.saveAll(roles);
        return getRoles(serverId);
    }

    // ──────────────── permissions ────────────────

    public List<PermissionResponse> getPermissions(UUID serverId, UUID roleId) {
        Role role = getRole(serverId, roleId);
        return Arrays.stream(Permission.values())
                .map(p -> PermissionResponse.builder()
                        .name(p.name())
                        .description(p.name())
                        .granted(Permission.hasPermission(role.getPermissions(), p))
                        .build())
                .toList();
    }

    @Transactional
    public List<PermissionResponse> updatePermissions(UUID serverId, UUID roleId,
                                                       UpdateRolePermissionsRequest request) {
        Role role = getRole(serverId, roleId);

        List<Permission> granted = request.getGrantedPermissions().stream()
                .map(Permission::valueOf)
                .toList();

        role.setPermissions(Permission.buildBitmask(granted));
        roleRepository.save(role);

        // Broadcast permission change to affected members in real time
        List<UUID> affectedUserIds;
        if (Boolean.TRUE.equals(role.getIsDefault())) {
            // @everyone role — broadcast to all server members
            affectedUserIds = serverMemberRepository.findAllByServerId(serverId)
                    .stream().map(ServerMember::getUserId).toList();
        } else {
            // Custom role — broadcast only to members assigned this role
            affectedUserIds = memberRoleRepository.findByRoleId(roleId)
                    .stream().map(mr -> mr.getMemberId()).toList();
        }
        ServerEventNotification event = ServerEventNotification.builder()
                .type("ROLE_PERMISSIONS_UPDATED")
                .serverId(serverId)
                .roleId(roleId)
                .build();
        affectedUserIds.forEach(uid ->
                messagingTemplate.convertAndSend("/topic/users/" + uid + "/server-events", event));

        return getPermissions(serverId, roleId);
    }

    // ──────────────── members ────────────────

    public List<MemberBriefResponse> getMembers(UUID serverId, UUID roleId) {
        getRole(serverId, roleId);
        return buildMemberList(roleId);
    }

    @Transactional
    public List<MemberBriefResponse> assignMembers(UUID serverId, UUID roleId,
                                                    AssignRoleMembersRequest request) {
        getRole(serverId, roleId);
        assignMembersInternal(roleId, serverId, request.getMemberIds());
        return buildMemberList(roleId);
    }

    @Transactional
    public void removeMember(UUID serverId, UUID roleId, UUID memberId) {
        getRole(serverId, roleId);
        memberRoleRepository.deleteByMemberIdAndRoleId(memberId, roleId);
    }

    /**
     * Returns all custom roles (non-default, non-@everyone) assigned to a specific user in a server.
     */
    public List<RoleResponse> getMemberRoles(UUID serverId, UUID userId) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<UUID> roleIds = memberRoleRepository.findRoleIdsByMemberId(member.getId());
        if (roleIds.isEmpty()) return List.of();

        return roleRepository.findAllById(roleIds).stream()
                .filter(role -> !Boolean.TRUE.equals(role.getIsDefault()))
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    // ──────────────── helpers ────────────────

    private Role getRole(UUID serverId, UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (!role.getServerId().equals(serverId)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private long resolvePreset(String preset) {
        if (preset == null) return 0L;
        return switch (preset.toUpperCase()) {
            case "MEMBER" -> PRESET_MEMBER;
            case "ADMIN" -> PRESET_ADMIN;
            default -> 0L;
        };
    }

    // ──────────────── effective permission helpers ────────────────

    /**
     * Returns the combined permissions bitmask for a user in a server.
     * Server owner always has all permissions.
     * Combines the @everyone default role + all custom roles assigned to the member.
     */
    public long getEffectivePermissions(UUID serverId, UUID userId) {
        Server server = serverRepository.findById(serverId).orElse(null);
        if (server == null) return 0L;
        if (server.getOwnerId().equals(userId)) return Permission.ALL;

        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerIdAndUserId(serverId, userId);
        if (memberOpt.isEmpty()) return 0L;

        ServerMember member = memberOpt.get();

        // Start with the @everyone default role permissions
        long perms = roleRepository.findByServerIdAndIsDefaultTrue(serverId)
                .map(Role::getPermissions).orElse(0L);

        // OR in each custom role
        List<UUID> roleIds = memberRoleRepository.findRoleIdsByMemberId(member.getId());
        if (!roleIds.isEmpty()) {
            perms = roleRepository.findAllById(roleIds).stream()
                    .mapToLong(Role::getPermissions)
                    .reduce(perms, (a, b) -> a | b);
        }
        return perms;
    }

    public boolean hasServerPermission(UUID serverId, UUID userId, Permission perm) {
        return Permission.hasPermission(getEffectivePermissions(serverId, userId), perm);
    }

    private void assignMembersInternal(UUID roleId, UUID serverId, List<UUID> userIds) {
        for (UUID userId : userIds) {
            ServerMember member = serverMemberRepository
                    .findByServerIdAndUserId(serverId, userId)
                    .orElse(null);
            if (member == null) continue;
            MemberRole.MemberRoleId id = new MemberRole.MemberRoleId(member.getId(), roleId);
            if (!memberRoleRepository.existsById(id)) {
                memberRoleRepository.save(MemberRole.builder()
                        .memberId(member.getId())
                        .roleId(roleId)
                        .build());
            }
        }
    }

    private List<MemberBriefResponse> buildMemberList(UUID roleId) {
        List<MemberRole> memberRoles = memberRoleRepository.findByRoleId(roleId);
        List<UUID> memberIds = memberRoles.stream().map(MemberRole::getMemberId).toList();

        if (memberIds.isEmpty()) return List.of();

        List<ServerMember> members = serverMemberRepository.findAllById(memberIds);
        List<UUID> userIds = members.stream().map(ServerMember::getUserId).toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(sm -> {
            User user = userMap.get(sm.getUserId());
            return MemberBriefResponse.builder()
                    .serverMemberId(sm.getId())
                    .userId(sm.getUserId())
                    .displayName(user != null ? user.getDisplayName() : null)
                    .username(user != null ? user.getUsername() : "Unknown")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .build();
        }).toList();
    }
}
