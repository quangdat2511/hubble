package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.ServerInviteRequest;
import com.hubble.dto.response.ServerInviteResponse;
import com.hubble.service.ServerInviteService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerInviteController {

    ServerInviteService serverInviteService;

    @PostMapping("/api/servers/{serverId}/invites")
    public ResponseEntity<ApiResponse<ServerInviteResponse>> inviteUser(
            @PathVariable UUID serverId,
            @Valid @RequestBody ServerInviteRequest request,
            Authentication authentication) {
        UUID inviterId = UUID.fromString(authentication.getName());
        ServerInviteResponse response = serverInviteService.inviteUser(inviterId, serverId, request);
        return ResponseEntity.ok(ApiResponse.<ServerInviteResponse>builder()
                .message("Invitation sent successfully")
                .result(response)
                .build());
    }

    @GetMapping("/api/servers/{serverId}/invites")
    public ResponseEntity<ApiResponse<List<ServerInviteResponse>>> getPendingInvitesByServer(
            @PathVariable UUID serverId,
            Authentication authentication) {
        UUID requestorId = UUID.fromString(authentication.getName());
        List<ServerInviteResponse> invites = serverInviteService.getPendingInvitesByServer(requestorId, serverId);
        return ResponseEntity.ok(ApiResponse.<List<ServerInviteResponse>>builder()
                .result(invites)
                .build());
    }

    @GetMapping("/api/invites/me")
    public ResponseEntity<ApiResponse<List<ServerInviteResponse>>> getMyPendingInvites(
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ServerInviteResponse> invites = serverInviteService.getMyPendingInvites(userId);
        return ResponseEntity.ok(ApiResponse.<List<ServerInviteResponse>>builder()
                .result(invites)
                .build());
    }

    @PutMapping("/api/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @PathVariable UUID inviteId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        serverInviteService.acceptInvite(userId, inviteId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Invitation accepted")
                .build());
    }

    @PutMapping("/api/invites/{inviteId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvite(
            @PathVariable UUID inviteId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        serverInviteService.declineInvite(userId, inviteId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Invitation declined")
                .build());
    }
}

