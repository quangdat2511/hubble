package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.FriendUserResponse;
import com.hubble.dto.response.FriendRequestResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.FriendService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/friends", "/api/contacts"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendController {

    FriendService friendService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FriendUserResponse>>> searchUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("q") String query
    ) {
        UUID currentUserId = principal.getId();
        List<FriendUserResponse> result = friendService.searchUsers(currentUserId, query);
        return ResponseEntity.ok(ApiResponse.<List<FriendUserResponse>>builder().result(result).build());
    }

    @PostMapping("/requests/{userId}")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        UUID currentUserId = principal.getId();
        FriendRequestResponse response = friendService.sendFriendRequest(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.<FriendRequestResponse>builder().result(response).build());
    }

    @PostMapping("/requests/username/{username}")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequestByUsername(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String username
    ) {
        UUID currentUserId = principal.getId();
        FriendRequestResponse response = friendService.sendFriendRequestByUsername(currentUserId, username);
        return ResponseEntity.ok(ApiResponse.<FriendRequestResponse>builder().result(response).build());
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal.getId();
        List<FriendRequestResponse> result = friendService.getIncomingRequests(currentUserId);
        return ResponseEntity.ok(ApiResponse.<List<FriendRequestResponse>>builder().result(result).build());
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getOutgoingRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal.getId();
        List<FriendRequestResponse> result = friendService.getOutgoingRequests(currentUserId);
        return ResponseEntity.ok(ApiResponse.<List<FriendRequestResponse>>builder().result(result).build());
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId
    ) {
        UUID currentUserId = principal.getId();
        friendService.acceptRequest(currentUserId, requestId);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đã chấp nhận lời mời").build());
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<String>> declineRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId
    ) {
        UUID currentUserId = principal.getId();
        friendService.declineRequest(currentUserId, requestId);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đã từ chối lời mời").build());
    }

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<List<FriendUserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal.getId();
        List<FriendUserResponse> result = friendService.getBlockedUsers(currentUserId);
        return ResponseEntity.ok(ApiResponse.<List<FriendUserResponse>>builder().result(result).build());
    }

    @PostMapping("/blocks/{userId}")
    public ResponseEntity<ApiResponse<String>> blockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        UUID currentUserId = principal.getId();
        friendService.blockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đã chặn người dùng").build());
    }

    @DeleteMapping("/blocks/{userId}")
    public ResponseEntity<ApiResponse<String>> unblockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        UUID currentUserId = principal.getId();
        friendService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đã bỏ chặn người dùng").build());
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<FriendUserResponse>>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal.getId();
        List<FriendUserResponse> result = friendService.getFriends(currentUserId);
        return ResponseEntity.ok(ApiResponse.<List<FriendUserResponse>>builder().result(result).build());
    }
}