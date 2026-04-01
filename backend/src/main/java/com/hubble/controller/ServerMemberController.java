package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.ServerMemberResponse;
import com.hubble.service.ServerMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerMemberController {
    ServerMemberService serverMemberService;

    @GetMapping("/{serverId}/members")
    public ResponseEntity<ApiResponse<List<ServerMemberResponse>>> getServerMembers(
            @PathVariable UUID serverId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ServerMemberResponse> members = serverMemberService.getServerMembers(userId, serverId);
        return ResponseEntity.ok(ApiResponse.<List<ServerMemberResponse>>builder()
                .result(members)
                .build());
    }

    @DeleteMapping("/{serverId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @PathVariable UUID serverId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        UUID requestorId = UUID.fromString(authentication.getName());
        serverMemberService.kickMember(requestorId, serverId, memberId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Member kicked successfully")
                .build());
    }

    @PutMapping("/{serverId}/owner/{memberId}")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable UUID serverId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        UUID requestorId = UUID.fromString(authentication.getName());
        serverMemberService.transferOwnership(requestorId, serverId, memberId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Ownership transferred successfully")
                .build());
    }
}
