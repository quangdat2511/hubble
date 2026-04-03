package com.hubble.controller;

import com.hubble.dto.response.SharedContentPageResponse;
import com.hubble.dto.response.SharedContentItemResponse;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageController.class)
class MessageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MessageService messageService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserRepository userRepository;

    @MockBean
    UserSessionRepository userSessionRepository;

    @Test
    @DisplayName("GET /api/messages/{channelId}/shared-content returns paged shared content")
    void shouldReturnSharedContentPage() throws Exception {
        UUID userId = UUID.randomUUID();
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

        when(messageService.getSharedContent(eq(userId.toString()), eq(channelId), eq(com.hubble.enums.SharedContentType.MEDIA), eq(0), eq(24)))
                .thenReturn(response);

        mockMvc.perform(get("/api/messages/{channelId}/shared-content", channelId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new UserPrincipal(userId)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("MEDIA"))
                .andExpect(jsonPath("$.result.items[0].filename").value("photo.png"))
                .andExpect(jsonPath("$.result.items[0].url").value("https://cdn.example.com/photo.png"));
    }
}
