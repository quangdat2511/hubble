package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ServerResponse;
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
                .build());
    }
}
