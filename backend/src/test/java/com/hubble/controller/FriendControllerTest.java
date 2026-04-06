package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.configuration.SecurityConfig;
import com.hubble.dto.response.FriendRequestResponse;
import com.hubble.dto.response.FriendUserResponse;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtAuthenticationFilter;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FriendService friendService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSessionRepository userSessionRepository;

    private UUID currentUserId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(currentUserId);
        authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    public void searchUsers_ValidQuery_ReturnsList() throws Exception {
        String query = "testuser";
        List<FriendUserResponse> mockResponse = List.of(
                FriendUserResponse.builder()
                        .id(UUID.randomUUID())
                        .username("testuser1")
                        .displayName("Test User 1")
                        .relationStatus("NONE")
                        .build()
        );

        when(friendService.searchUsers(currentUserId, query)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/friends/search")
                        .param("q", query)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].username").value("testuser1"));
    }

    @Test
    public void searchUsers_EmptyQuery_ReturnsEmptyList() throws Exception {
        when(friendService.searchUsers(currentUserId, "")).thenReturn(List.of());

        mockMvc.perform(get("/api/friends/search")
                        .param("q", "")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    public void sendFriendRequest_ValidUserId_ReturnsResponse() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        FriendRequestResponse mockResponse = FriendRequestResponse.builder()
                .id(UUID.randomUUID())
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .incoming(false)
                .build();

        when(friendService.sendFriendRequest(currentUserId, targetUserId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/friends/requests/{userId}", targetUserId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("PENDING"));
    }

    @Test
    public void sendFriendRequest_AlreadyFriends_ReturnsBadRequest() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        doThrow(new AppException(ErrorCode.ALREADY_FRIENDS))
                .when(friendService).sendFriendRequest(currentUserId, targetUserId);

        mockMvc.perform(post("/api/friends/requests/{userId}", targetUserId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.ALREADY_FRIENDS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ALREADY_FRIENDS.getMessage()));
    }

    @Test
    public void sendFriendRequestByUsername_ValidUsername_ReturnsResponse() throws Exception {
        String targetUsername = "targetuser";
        FriendRequestResponse mockResponse = FriendRequestResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .build();

        when(friendService.sendFriendRequestByUsername(currentUserId, targetUsername)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/friends/requests/username/{username}", targetUsername)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("PENDING"));
    }

    @Test
    public void getIncomingRequests_ReturnsList() throws Exception {
        List<FriendRequestResponse> mockResponse = List.of(
                FriendRequestResponse.builder().id(UUID.randomUUID()).incoming(true).status("PENDING").build()
        );

        when(friendService.getIncomingRequests(currentUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/friends/requests/received")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].incoming").value(true));
    }

    @Test
    public void getOutgoingRequests_ReturnsList() throws Exception {
        List<FriendRequestResponse> mockResponse = List.of(
                FriendRequestResponse.builder().id(UUID.randomUUID()).incoming(false).status("PENDING").build()
        );

        when(friendService.getOutgoingRequests(currentUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/friends/requests/sent")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].incoming").value(false));
    }

    @Test
    public void acceptRequest_ValidRequestId_ReturnsOk() throws Exception {
        UUID requestId = UUID.randomUUID();
        doNothing().when(friendService).acceptRequest(currentUserId, requestId);

        mockMvc.perform(post("/api/friends/requests/{requestId}/accept", requestId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Đã chấp nhận lời mời"));
    }

    @Test
    public void declineRequest_ValidRequestId_ReturnsOk() throws Exception {
        UUID requestId = UUID.randomUUID();
        doNothing().when(friendService).declineRequest(currentUserId, requestId);

        mockMvc.perform(delete("/api/friends/requests/{requestId}", requestId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Đã từ chối lời mời"));
    }

    @Test
    public void getBlockedUsers_ReturnsList() throws Exception {
        List<FriendUserResponse> mockResponse = List.of(
                FriendUserResponse.builder().id(UUID.randomUUID()).relationStatus("BLOCKED_BY_ME").build()
        );

        when(friendService.getBlockedUsers(currentUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/friends/blocks")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].relationStatus").value("BLOCKED_BY_ME"));
    }

    @Test
    public void blockUser_ValidUserId_ReturnsOk() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        doNothing().when(friendService).blockUser(currentUserId, targetUserId);

        mockMvc.perform(post("/api/friends/blocks/{userId}", targetUserId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Đã chặn người dùng"));
    }

    @Test
    public void unblockUser_ValidUserId_ReturnsOk() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        doNothing().when(friendService).unblockUser(currentUserId, targetUserId);

        mockMvc.perform(delete("/api/friends/blocks/{userId}", targetUserId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Đã bỏ chặn người dùng"));
    }

    @Test
    public void getFriends_ReturnsList() throws Exception {
        List<FriendUserResponse> mockResponse = List.of(
                FriendUserResponse.builder()
                        .id(UUID.randomUUID())
                        .username("friend1")
                        .relationStatus("FRIEND")
                        .build()
        );

        when(friendService.getFriends(currentUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/friends/friends")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].relationStatus").value("FRIEND"));
    }
}