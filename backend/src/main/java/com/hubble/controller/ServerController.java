package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateChannelRequest;
import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.request.UpdateChannelRequest;
import com.hubble.dto.response.ChannelMemberResponse;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ChannelRoleResponse;
import com.hubble.dto.response.ServerResponse;
import com.hubble.service.ChannelService;
import com.hubble.service.ServerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerController {

    ServerService serverService;
    ChannelService channelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic,
            @RequestPart(value = "icon", required = false) MultipartFile iconFile,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        CreateServerRequest request = CreateServerRequest.builder()
                .name(name)
                .description(description)
                .isPublic(isPublic)
                .build();
        return ResponseEntity.ok(ApiResponse.<ServerResponse>builder()
                .result(serverService.createServer(userId, request, iconFile))
                .build());
    }

    @PutMapping(value = "/{serverId}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ServerResponse>> updateServerIcon(
            @PathVariable UUID serverId,
            @RequestPart("icon") MultipartFile iconFile,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<ServerResponse>builder()
                .result(serverService.updateServerIcon(userId, serverId, iconFile))
                .build());
    }

    @DeleteMapping("/{serverId}/icon")
    public ResponseEntity<ApiResponse<ServerResponse>> removeServerIcon(
            @PathVariable UUID serverId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<ServerResponse>builder()
                .result(serverService.removeServerIcon(userId, serverId))
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getMyServers(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<List<ServerResponse>>builder()
                .result(serverService.getMyServers(userId))
                .build());
    }

    @GetMapping("/{serverId}/channels")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getServerChannels(
            @PathVariable UUID serverId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<List<ChannelResponse>>builder()
                .result(serverService.getServerChannels(serverId, userId))
                .result(serverService.getServerChannels(serverId, userId))
                .build());
    }

    @PostMapping("/{serverId}/channels")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @PathVariable UUID serverId,
            @RequestBody CreateChannelRequest request,
            Authentication authentication) {
        UUID creatorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<ChannelResponse>builder()
                .result(channelService.createChannel(serverId, creatorId, request))
                .build());
    }

    @PutMapping("/{serverId}/channels/{channelId}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            @RequestBody UpdateChannelRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ChannelResponse>builder()
                .result(channelService.updateChannel(channelId, request))
                .build());
    }

    @DeleteMapping("/{serverId}/channels/{channelId}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            Authentication authentication) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @GetMapping("/{serverId}/channels/{channelId}/members")
    public ResponseEntity<ApiResponse<List<ChannelMemberResponse>>> getChannelMembers(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ChannelMemberResponse>>builder()
                .result(channelService.getChannelMembers(channelId))
                .build());
    }

    @GetMapping("/{serverId}/channels/{channelId}/roles")
    public ResponseEntity<ApiResponse<List<ChannelRoleResponse>>> getChannelRoles(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ChannelRoleResponse>>builder()
                .result(channelService.getChannelRoles(channelId))
                .build());
    }

    @PostMapping("/{serverId}/channels/{channelId}/members")
    public ResponseEntity<ApiResponse<Void>> addChannelMembers(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            @RequestBody List<UUID> userIds,
            Authentication authentication) {
        channelService.addChannelMembers(channelId, userIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @PostMapping("/{serverId}/channels/{channelId}/roles")
    public ResponseEntity<ApiResponse<Void>> addChannelRoles(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            @RequestBody List<UUID> roleIds,
            Authentication authentication) {
        channelService.addChannelRoles(channelId, roleIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @DeleteMapping("/{serverId}/channels/{channelId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeChannelMember(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            @PathVariable UUID userId,
            Authentication authentication) {
        channelService.removeChannelMember(channelId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @DeleteMapping("/{serverId}/channels/{channelId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> removeChannelRole(
            @PathVariable UUID serverId,
            @PathVariable UUID channelId,
            @PathVariable UUID roleId,
            Authentication authentication) {
        channelService.removeChannelRole(channelId, roleId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}
