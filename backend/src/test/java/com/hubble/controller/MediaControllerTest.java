package com.hubble.controller;

import com.hubble.dto.response.UploadResponse;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtService;
import com.hubble.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MediaController.class)
class MediaControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    MediaService mediaService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserRepository userRepository;

    @MockBean
    UserSessionRepository userSessionRepository;

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[2048]
        );
    }

    @Nested
    @DisplayName("POST /api/media/upload")
    class Upload {

        @Test
        @DisplayName("200 OK with UploadResponse when file is valid")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void shouldReturn200WhenFileIsValid() throws Exception {
            UploadResponse mockResponse = UploadResponse.builder()
                    .attachmentId(UUID.randomUUID())
                    .url("http://localhost:9000/hubble-media/media/uuid.jpg")
                    .filename("photo.jpg")
                    .contentType("image/jpeg")
                    .sizeBytes(2048L)
                    .build();

            when(mediaService.uploadMedia(any(), eq("media"))).thenReturn(mockResponse);

            mockMvc.perform(multipart("/api/media/upload").file(validFile()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.url").value(mockResponse.getUrl()))
                    .andExpect(jsonPath("$.result.filename").value("photo.jpg"))
                    .andExpect(jsonPath("$.result.contentType").value("image/jpeg"))
                    .andExpect(jsonPath("$.result.sizeBytes").value(2048))
                    .andExpect(jsonPath("$.result.attachmentId").isNotEmpty());
        }

        @Test
        @DisplayName("400 when service throws FILE_TOO_LARGE")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void shouldReturn400WhenFileTooLarge() throws Exception {
            when(mediaService.uploadMedia(any(), any()))
                    .thenThrow(new AppException(ErrorCode.FILE_TOO_LARGE));

            mockMvc.perform(multipart("/api/media/upload").file(validFile()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when service throws FILE_TYPE_NOT_ALLOWED")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void shouldReturn400WhenTypeNotAllowed() throws Exception {
            when(mediaService.uploadMedia(any(), any()))
                    .thenThrow(new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED));

            mockMvc.perform(multipart("/api/media/upload").file(validFile()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 when storage service fails")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void shouldReturn500WhenStorageFails() throws Exception {
            when(mediaService.uploadMedia(any(), any()))
                    .thenThrow(new AppException(ErrorCode.UPLOAD_FAILED));

            mockMvc.perform(multipart("/api/media/upload").file(validFile()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

    }
}