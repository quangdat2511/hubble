package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {
    @Mock
    MinioClient minioClient;
    @Mock
    StorageProperties storageProperties;
    @InjectMocks
    MinioStorageService minioStorageService;



    @Test
    @DisplayName("upload() should return a URL containing the public base URL")
    void shouldReturnPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpeg", "image/jpeg", new byte[512]
        );
        when(storageProperties.getBucket()).thenReturn("hubble-media");
        when(storageProperties.getPublicUrl()).thenReturn("http://localhost:9000/hubble-media");

        String url = minioStorageService.upload(file, "media");

        assertThat(url).startsWith("http://localhost:9000/hubble-media/media/");
        assertThat(url).endsWith(".jpeg");
    }

    @Test
    @DisplayName("upload() should fall back to 'bin' extension for files without extension")
    void shouldFallbackToBinExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "application/octet-stream", new byte[128]
        );
        when(storageProperties.getBucket()).thenReturn("hubble-media");
        when(storageProperties.getPublicUrl()).thenReturn("http://localhost:9000/hubble-media");

        String url = minioStorageService.upload(file, "media");

        assertThat(url).endsWith(".bin");
    }
    @Test
    @DisplayName("upload() should propagate exception when MinIO throws")
    void shouldPropagateMinioException() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpeg", "image/jpeg", new byte[512]
        );

        when(storageProperties.getBucket()).thenReturn("hubble-media");

        doThrow(new RuntimeException("bucket not found"))
                .when(minioClient)
                .putObject(any());


        assertThatThrownBy(() -> minioStorageService.upload(file, "media"))
                .isInstanceOf(RuntimeException.class);
    }
}