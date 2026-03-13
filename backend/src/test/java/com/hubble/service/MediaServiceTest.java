package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import com.hubble.dto.response.UploadResponse;
import com.hubble.entity.Attachment;
import com.hubble.exception.AppException;
import com.hubble.repository.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock
    StorageService storageService;

    @Mock
    AttachmentRepository attachmentRepository;

    @Mock
    StorageProperties storageProperties;

    @InjectMocks
    MediaService mediaService;

    private MockMultipartFile getValidImageFile() {
        return new MockMultipartFile(
                "file",
                "photo.jpeg",
                "image/jpeg",
                new byte[1024]
        );
    }


    @Test
    void shouldUploadAndReturnResponse() throws Exception {
        MockMultipartFile file = getValidImageFile();
        String expectedUrl = "http://localhost:9000/hubble-media/media/uuid.jpg";

        when(storageService.upload(any(), eq("media"))).thenReturn(expectedUrl);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(storageProperties.getMaxFileSizeMb()).thenReturn(25);
        when(storageProperties.getAllowedTypes()).thenReturn(List.of(
                "image/jpeg", "image/png", "image/gif",
                "video/mp4", "audio/mpeg", "application/pdf"
        ));

        UploadResponse response = mediaService.uploadMedia(file, "media");

        assertThat(response.getUrl()).isEqualTo(expectedUrl);
        assertThat(response.getFilename()).isEqualTo("photo.jpeg");
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getSizeBytes()).isEqualTo(1024L);
        assertThat(response.getAttachmentId()).isNotNull();

        verify(storageService, times(1)).upload(any(), eq("media"));
        verify(attachmentRepository, times(1)).save(any());
    }

    @Test
    void savedAttachmentShouldHaveNullMessageId() throws Exception {
        // Arrange
        when(storageService.upload(any(), any())).thenReturn("http://url");
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(storageProperties.getMaxFileSizeMb()).thenReturn(25);
        when(storageProperties.getAllowedTypes()).thenReturn(List.of(
                "image/jpeg", "image/png", "image/gif",
                "video/mp4", "audio/mpeg", "application/pdf"
        ));

        // Act
        mediaService.uploadMedia(getValidImageFile(), "media");

        // Assert
        var captor = org.mockito.ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageId()).isNull();
    }



    @Nested
    class ValidationFailures {
        @Test
        void shouldThrowWhenFileIsNull() {
            assertThatThrownBy(() -> mediaService.uploadMedia(null, "media"))
                    .isInstanceOf(AppException.class);

            verifyNoInteractions(storageService, attachmentRepository);
        }

        @Test
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]
            );

            assertThatThrownBy(() -> mediaService.uploadMedia(emptyFile, "media"))
                    .isInstanceOf(AppException.class);

            verifyNoInteractions(storageService);
        }

        @Test
        void shouldThrowWhenFileTooLarge() {
            long oversizedBytes = 26L * 1024 * 1024; // 26 MB > 25 MB limit
            MockMultipartFile bigFile = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", new byte[(int) oversizedBytes]
            );

            assertThatThrownBy(() -> mediaService.uploadMedia(bigFile, "media"))
                    .isInstanceOf(AppException.class);

            verifyNoInteractions(storageService);
        }

        @Test
        void shouldThrowWhenContentTypeNotAllowed() {
            MockMultipartFile exeFile = new MockMultipartFile(
                    "file", "virus.exe", "application/x-msdownload", new byte[512]
            );

            assertThatThrownBy(() -> mediaService.uploadMedia(exeFile, "media"))
                    .isInstanceOf(AppException.class);

            verifyNoInteractions(storageService);
        }
    }


}