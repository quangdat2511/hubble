package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.dto.request.PushConfigUpdateRequest;
import com.hubble.dto.response.PushConfigResponse;
import com.hubble.repository.UserRepository;
import com.hubble.security.JwtService;
import com.hubble.service.PushConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PushConfigController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class PushConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PushConfigService pushConfigService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getConfig_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);

        when(pushConfigService.getPushConfig(userId))
                .thenReturn(PushConfigResponse.builder()
                        .notificationEnabled(true)
                        .notificationSound(false)
                        .build());

        mockMvc.perform(get("/api/settings/push")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.notificationEnabled").value(true))
                .andExpect(jsonPath("$.result.notificationSound").value(false));
    }

    @Test
    void updateConfig_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        PushConfigUpdateRequest request = PushConfigUpdateRequest.builder()
                .notificationEnabled(false)
                .notificationSound(false)
                .build();

        when(pushConfigService.updatePushConfig(any(UUID.class), any(PushConfigUpdateRequest.class)))
                .thenReturn(PushConfigResponse.builder()
                        .notificationEnabled(false)
                        .notificationSound(false)
                        .build());

        mockMvc.perform(put("/api/settings/push")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.notificationEnabled").value(false))
                .andExpect(jsonPath("$.result.notificationSound").value(false));
    }
}
