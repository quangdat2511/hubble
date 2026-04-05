package com.hubble.controller;

import com.hubble.dto.response.NotificationResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private final UUID userId = UUID.randomUUID();
    private UserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = mock(UserPrincipal.class);
        lenient().when(mockPrincipal.getId()).thenReturn(userId);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockPrincipal;
                    }
                })
                .build();
    }

    @Test
    void getNotifications_ShouldReturnList() throws Exception {
        NotificationResponse response = NotificationResponse.builder().id(UUID.randomUUID()).content("Test").build();
        when(notificationService.getUserNotifications(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].content").value("Test"));

        verify(notificationService).getUserNotifications(userId, 0, 20);
    }

    @Test
    void getUnreadCount_ShouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));

        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void markAsRead_ShouldReturnSuccess() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(userId, notificationId);
    }

    @Test
    void markAllAsRead_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(userId);
    }
}