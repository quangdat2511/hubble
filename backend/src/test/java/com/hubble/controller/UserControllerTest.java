package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.configuration.SecurityConfig;
import com.hubble.dto.request.UpdateCustomStatusRequest;
import com.hubble.dto.request.UpdateProfileRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.enums.UserStatus;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtAuthenticationFilter;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.UserService;
import com.hubble.service.UserStatusService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserStatusService userStatusService;

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
    public void getMyProfile_ReturnsUserResponse() throws Exception {
        UserResponse mockResponse = UserResponse.builder()
                .id(currentUserId)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .status("ONLINE")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(currentUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(currentUserId.toString()))
                .andExpect(jsonPath("$.result.username").value("testuser"))
                .andExpect(jsonPath("$.result.email").value("test@example.com"));
    }

    @Test
    public void getUserById_ValidId_ReturnsUserResponse() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UserResponse mockResponse = UserResponse.builder()
                .id(targetUserId)
                .username("targetuser")
                .displayName("Target User")
                .status("OFFLINE")
                .build();

        when(userService.getUserById(targetUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/{userId}", targetUserId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.result.username").value("targetuser"));
    }

    @Test
    public void updateProfile_ValidRequest_ReturnsUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Updated Name");
        request.setBio("This is my new bio");
        request.setPhone("0123456789");
        request.setStatus(UserStatus.DND);

        UserResponse mockResponse = UserResponse.builder()
                .id(currentUserId)
                .username("testuser")
                .displayName("Updated Name")
                .bio("This is my new bio")
                .phone("0123456789")
                .status("DND")
                .build();

        when(userService.updateProfile(eq(currentUserId), any(UpdateProfileRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/users/me")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.result.bio").value("This is my new bio"))
                .andExpect(jsonPath("$.result.status").value("DND"));
    }

    @Test
    public void updateCustomStatus_ValidRequest_ReturnsUpdatedUser() throws Exception {
        UpdateCustomStatusRequest request = new UpdateCustomStatusRequest();
        request.setCustomStatus("Working from home");

        UserResponse mockResponse = UserResponse.builder()
                .id(currentUserId)
                .username("testuser")
                .customStatus("Working from home")
                .build();

        when(userService.updateCustomStatus(eq(currentUserId), any(UpdateCustomStatusRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/users/me/custom-status")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.customStatus").value("Working from home"));
    }
}