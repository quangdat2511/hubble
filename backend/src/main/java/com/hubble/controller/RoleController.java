package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.*;
import com.hubble.dto.response.*;
import com.hubble.service.RoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers/{serverId}/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(@PathVariable UUID serverId) {
        return ResponseEntity.ok(ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getRoles(serverId))
                .build());
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleDetail(
            @PathVariable UUID serverId, @PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.<RoleDetailResponse>builder()
                .result(roleService.getRoleDetail(serverId, roleId))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @PathVariable UUID serverId, @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .result(roleService.createRole(serverId, request))
                .build());
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID serverId, @PathVariable UUID roleId,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .result(roleService.updateRole(serverId, roleId, request))
                .build());
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID serverId, @PathVariable UUID roleId) {
        roleService.deleteRole(serverId, roleId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> reorderRoles(
            @PathVariable UUID serverId, @Valid @RequestBody ReorderRolesRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.reorderRoles(serverId, request))
                .build());
    }

    // ──────────────── permissions ────────────────

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions(
            @PathVariable UUID serverId, @PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .result(roleService.getPermissions(serverId, roleId))
                .build());
    }

    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> updatePermissions(
            @PathVariable UUID serverId, @PathVariable UUID roleId,
            @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .result(roleService.updatePermissions(serverId, roleId, request))
                .build());
    }

    // ──────────────── members ────────────────

    @GetMapping("/{roleId}/members")
    public ResponseEntity<ApiResponse<List<MemberBriefResponse>>> getMembers(
            @PathVariable UUID serverId, @PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.<List<MemberBriefResponse>>builder()
                .result(roleService.getMembers(serverId, roleId))
                .build());
    }

    @PostMapping("/{roleId}/members")
    public ResponseEntity<ApiResponse<List<MemberBriefResponse>>> assignMembers(
            @PathVariable UUID serverId, @PathVariable UUID roleId,
            @RequestBody AssignRoleMembersRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<MemberBriefResponse>>builder()
                .result(roleService.assignMembers(serverId, roleId, request))
                .build());
    }

    @DeleteMapping("/{roleId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID serverId, @PathVariable UUID roleId,
            @PathVariable UUID memberId) {
        roleService.removeMember(serverId, roleId, memberId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}
