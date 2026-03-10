package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import com.hubble.repository.AttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

        when(storageService.upload(file, "media")).thenReturn(expectedUrl);
        when(attachmentRepository.save())

    }


}