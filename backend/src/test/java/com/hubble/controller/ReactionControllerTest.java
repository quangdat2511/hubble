package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.configuration.SecurityConfig;
import com.hubble.dto.request.ToggleReactionRequest;
import com.hubble.dto.response.ReactionResponse;
import com.hubble.entity.Message;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtAuthenticationFilter;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.ReactionService;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ReactionControllerTest.MessagingTestBeans.class})
class ReactionControllerTest {

    @TestConfiguration
    static class MessagingTestBeans {
        @Bean
        SimpMessagingTemplate simpMessagingTemplate() {
            return Mockito.spy(MessagingTestSupport.createTemplate());
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired SimpMessagingTemplate messagingTemplate;

    @MockBean ReactionService reactionService;
    @MockBean MessageRepository messageRepository;
    @MockBean JwtService jwtService;
    @MockBean UserRepository userRepository;
    @MockBean UserSessionRepository userSessionRepository;

    UUID userId;
    UUID channelId;
    Authentication authentication;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        channelId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId);
        authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void toggleReaction_persistsAndBroadcastsToReactionsTopic() throws Exception {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(
                Message.builder().id(messageId).channelId(channelId).build()
        ));
        List<ReactionResponse> reactions = List.of(
                ReactionResponse.builder().emoji("🎉").count(1).userIds(List.of(userId.toString())).build()
        );
        when(reactionService.toggleReaction(eq(messageId), eq(userId), eq("🎉"))).thenReturn(reactions);

        mockMvc.perform(put("/api/messages/{messageId}/reactions", messageId)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ToggleReactionRequest.builder().emoji("🎉").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].emoji").value("🎉"));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/channels/" + channelId + "/reactions"),
                any(Object.class)
        );
    }
}
