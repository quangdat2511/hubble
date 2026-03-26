package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ServerResponse;
import com.hubble.service.ServerService;
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
public class ServerController {

    ServerService serverService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @RequestBody CreateServerRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ServerResponse serverResponse = serverService.createServer(userId, request);
        return ResponseEntity.ok(ApiResponse.<ServerResponse>builder()
                .result(serverResponse)
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getMyServers(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ServerResponse> servers = serverService.getMyServers(userId);
        return ResponseEntity.ok(ApiResponse.<List<ServerResponse>>builder()
                .result(servers)
                .build());
    }

    @GetMapping("/{serverId}/channels")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getServerChannels(
            @PathVariable UUID serverId,
            Authentication authentication) {
        List<ChannelResponse> channels = serverService.getServerChannels(serverId);
        return ResponseEntity.ok(ApiResponse.<List<ChannelResponse>>builder()
                .result(channels)
                .build());
    }
}
