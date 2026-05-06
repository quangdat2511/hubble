package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.dto.request.AppLockSettingsRequest;
import com.hubble.dto.response.AppLockSettingsResponse;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtService;
import com.hubble.service.AppLockSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AppLockSettingsController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class AppLockSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppLockSettingsService appLockSettingsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Test
    void getSettings_Authenticated_ReturnsCurrentPinState() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(appLockSettingsService.getAppLockSettings(userId))
                .thenReturn(AppLockSettingsResponse.builder()
                        .appLockPin("1234")
                        .pinConfigured(true)
                        .build());

        mockMvc.perform(get("/api/settings/app-lock")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.appLockPin").value("1234"))
                .andExpect(jsonPath("$.result.pinConfigured").value(true));
    }

    @Test
    void updateSettings_Authenticated_ReturnsUpdatedPinState() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(appLockSettingsService.updateAppLockPin(userId, "5678"))
                .thenReturn(AppLockSettingsResponse.builder()
                        .appLockPin("5678")
                        .pinConfigured(true)
                        .build());

        mockMvc.perform(put("/api/settings/app-lock")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AppLockSettingsRequest.builder()
                                .appLockPin("5678")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.appLockPin").value("5678"))
                .andExpect(jsonPath("$.result.pinConfigured").value(true));
    }

    @Test
    void updateSettings_InvalidPin_ReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(appLockSettingsService.updateAppLockPin(eq(userId), eq("99")))
                .thenThrow(new AppException(ErrorCode.INVALID_APP_LOCK_PIN));

        mockMvc.perform(put("/api/settings/app-lock")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AppLockSettingsRequest.builder()
                                .appLockPin("99")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_APP_LOCK_PIN.getCode()));
    }
}
