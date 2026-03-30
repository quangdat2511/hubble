package com.hubble.controller;

import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtService;
import com.hubble.service.LanguageService;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = LanguageController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class LanguageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LanguageService languageService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Test
    void getLanguage_ReturnsWrappedLocale() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(languageService.getLanguage(userId)).thenReturn("vi");

        mockMvc.perform(get("/api/settings/language").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("vi"));
    }

    @Test
    void updateLanguage_ReturnsUpdatedLocale() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(languageService.getLanguage(userId)).thenReturn("en");

        mockMvc.perform(put("/api/settings/language")
                        .principal(authentication)
                        .param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Language updated successfully"))
                .andExpect(jsonPath("$.result").value("en"));
    }

    @Test
    void updateLanguage_RejectsUnsupportedLocale() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        doThrow(new AppException(ErrorCode.LOCALE_INVALID))
                .when(languageService).updateLanguage(userId, "fr");

        mockMvc.perform(put("/api/settings/language")
                        .principal(authentication)
                        .param("locale", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.LOCALE_INVALID.getCode()));
    }
}
