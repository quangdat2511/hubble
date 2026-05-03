package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.configuration.SecurityConfig;
import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.MarkChannelReadRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.response.PeerReadStatusResponse;
import com.hubble.dto.response.SharedContentItemResponse;
import com.hubble.dto.response.SharedContentPageResponse;
import com.hubble.enums.SharedContentType;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtAuthenticationFilter;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.ChannelReadService;
import com.hubble.service.MessageService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MessageControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean MessageService messageService;
    @MockBean ChannelReadService channelReadService;
    @MockBean JwtService jwtService;
    @MockBean UserRepository userRepository;
    @MockBean UserSessionRepository userSessionRepository;

    UUID userId;
    Authentication authentication;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId);
        authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getMessages_delegatesToService() throws Exception {
        UUID channelId = UUID.randomUUID();
        when(messageService.getMessages(eq(userId.toString()), eq(channelId.toString()), eq(0), eq(30)))
                .thenReturn(List.of(MessageResponse.builder().id("m1").content("hi").build()));

        mockMvc.perform(get("/api/messages/{channelId}", channelId)
                        .param("page", "0")
                        .param("size", "30")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].content").value("hi"));
    }

    @Test
    void sendMessage_delegatesToService() throws Exception {
        CreateMessageRequest body = CreateMessageRequest.builder()
                .channelId(UUID.randomUUID().toString())
                .content("hello")
                .build();
        when(messageService.sendMessage(eq(userId.toString()), any(CreateMessageRequest.class)))
                .thenReturn(MessageResponse.builder().id("new").content("hello").build());

        mockMvc.perform(post("/api/messages")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("new"));

        verify(messageService).sendMessage(eq(userId.toString()), any(CreateMessageRequest.class));
    }

    @Test
    void markChannelRead_delegatesToChannelReadService() throws Exception {
        UUID channelId = UUID.randomUUID();
        MarkChannelReadRequest body = MarkChannelReadRequest.builder()
                .messageId(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/api/messages/channel/{channelId}/read", channelId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(channelReadService).markRead(eq(userId), eq(channelId.toString()), any(MarkChannelReadRequest.class));
    }

    @Test
    void getPeerReadStatus_returnsPayload() throws Exception {
        UUID channelId = UUID.randomUUID();
        when(channelReadService.getPeerReadStatus(userId, channelId.toString()))
                .thenReturn(PeerReadStatusResponse.builder().readAt("2026-04-01T12:00:00").build());

        mockMvc.perform(get("/api/messages/channel/{channelId}/peer-read-status", channelId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.readAt").value("2026-04-01T12:00:00"));
    }

    @Test
    void getSharedContent_returnsPagedPayload() throws Exception {
        String channelId = UUID.randomUUID().toString();
        SharedContentPageResponse response = SharedContentPageResponse.builder()
                .type("MEDIA")
                .page(0)
                .size(24)
                .hasMore(false)
                .items(List.of(SharedContentItemResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .type("MEDIA")
                        .url("https://cdn.example.com/photo.png")
                        .filename("photo.png")
                        .build()))
                .build();

        when(messageService.getSharedContent(
                eq(userId.toString()),
                eq(channelId),
                eq(SharedContentType.MEDIA),
                eq(0),
                eq(24)))
                .thenReturn(response);

        mockMvc.perform(get("/api/messages/{channelId}/shared-content", channelId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("MEDIA"))
                .andExpect(jsonPath("$.result.items[0].filename").value("photo.png"))
                .andExpect(jsonPath("$.result.items[0].url").value("https://cdn.example.com/photo.png"));
    }

    @Test
    void editMessage_delegatesToService() throws Exception {
        UUID messageId = UUID.randomUUID();
        when(messageService.editMessage(eq(userId.toString()), eq(messageId.toString()), any(UpdateMessageRequest.class)))
                .thenReturn(MessageResponse.builder().id(messageId.toString()).content("edited").build());

        UpdateMessageRequest body = new UpdateMessageRequest();
        body.setContent("edited");
        mockMvc.perform(patch("/api/messages/{messageId}", messageId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").value("edited"));
    }

    @Test
    void unsendMessage_delegatesToService() throws Exception {
        UUID messageId = UUID.randomUUID();
        when(messageService.unsendMessage(userId.toString(), messageId.toString()))
                .thenReturn(MessageResponse.builder().id(messageId.toString()).isDeleted(true).build());

        mockMvc.perform(delete("/api/messages/{messageId}", messageId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isDeleted").value(true));
    }
}
