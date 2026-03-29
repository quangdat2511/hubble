package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.FriendRequestResponse;
import com.hubble.dto.response.FriendUserResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendControllerTest {

    @Mock
    private FriendService friendService;

    @InjectMocks
    private FriendController friendController;

    private UserPrincipal principal;
    private UUID currentUserId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        principal = new UserPrincipal(currentUserId);
    }

    @Test
    void searchUsers_ReturnsOk() {
        List<FriendUserResponse> mockResponse = List.of(new FriendUserResponse());
        when(friendService.searchUsers(currentUserId, "test")).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<List<FriendUserResponse>>> response = friendController.searchUsers(principal, "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().getResult());
    }

    @Test
    void sendFriendRequest_ReturnsOk() {
        FriendRequestResponse mockResponse = new FriendRequestResponse();
        when(friendService.sendFriendRequest(currentUserId, targetUserId)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<FriendRequestResponse>> response = friendController.sendFriendRequest(principal, targetUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().getResult());
    }

    @Test
    void sendFriendRequestByUsername_ReturnsOk() {
        FriendRequestResponse mockResponse = new FriendRequestResponse();
        when(friendService.sendFriendRequestByUsername(currentUserId, "john_doe")).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<FriendRequestResponse>> response = friendController.sendFriendRequestByUsername(principal, "john_doe");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().getResult());
    }

    @Test
    void getBlockedUsers_ReturnsOk() {
        List<FriendUserResponse> mockResponse = List.of(new FriendUserResponse());
        when(friendService.getBlockedUsers(currentUserId)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<List<FriendUserResponse>>> response = friendController.getBlockedUsers(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().getResult());
    }

    @Test
    void blockUser_ReturnsOk() {
        doNothing().when(friendService).blockUser(currentUserId, targetUserId);

        ResponseEntity<ApiResponse<String>> response = friendController.blockUser(principal, targetUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Đã chặn người dùng", response.getBody().getResult());
        verify(friendService).blockUser(currentUserId, targetUserId);
    }

    @Test
    void unblockUser_ReturnsOk() {
        doNothing().when(friendService).unblockUser(currentUserId, targetUserId);

        ResponseEntity<ApiResponse<String>> response = friendController.unblockUser(principal, targetUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Đã bỏ chặn người dùng", response.getBody().getResult());
        verify(friendService).unblockUser(currentUserId, targetUserId);
    }

    @Test
    void acceptRequest_ReturnsOk() {
        UUID requestId = UUID.randomUUID();
        doNothing().when(friendService).acceptRequest(currentUserId, requestId);

        ResponseEntity<ApiResponse<String>> response = friendController.acceptRequest(principal, requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(friendService).acceptRequest(currentUserId, requestId);
    }

    @Test
    void declineRequest_ReturnsOk() {
        UUID requestId = UUID.randomUUID();
        doNothing().when(friendService).declineRequest(currentUserId, requestId);

        ResponseEntity<ApiResponse<String>> response = friendController.declineRequest(principal, requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(friendService).declineRequest(currentUserId, requestId);
    }
}