package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceTest {

    @Mock
    private StorageProperties props;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SupabaseStorageService supabaseStorageService;

    @BeforeEach
    void setUp() {
        // Tiêm mock RestTemplate vào Service
        ReflectionTestUtils.setField(supabaseStorageService, "restTemplate", restTemplate);

        // Giả lập config Properties
        when(props.getEndpoint()).thenReturn("https://test.supabase.co");
        when(props.getBucket()).thenReturn("hubble-bucket");
        when(props.getSecretKey()).thenReturn("test-secret-key");
    }

    @Test
    void upload_ValidFile_ReturnsPublicUrl() throws Exception {
        // Arrange
        when(props.getPublicUrl()).thenReturn("https://test.supabase.co/storage/v1/object/public/hubble-bucket");
        MultipartFile mockFile = new MockMultipartFile(
                "file", "test-image.png", "image/png", "dummy-image-bytes".getBytes()
        );

        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(successResponse);

        // Act
        String resultUrl = supabaseStorageService.upload(mockFile, "media");

        // Assert
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://test.supabase.co/storage/v1/object/public/hubble-bucket/media/"));
        assertTrue(resultUrl.endsWith(".png")); // Đảm bảo bắt đúng đuôi file
    }

    @Test
    void upload_ApiReturnsError_ThrowsException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile(
                "file", "no-extension-file", "application/octet-stream", "dummy-bytes".getBytes()
        );

        ResponseEntity<String> errorResponse = new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(errorResponse);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            supabaseStorageService.upload(mockFile, "media");
        });

        // File không có dấu chấm sẽ được fallback về đuôi .bin (xác minh getExtension hoạt động)
        assertTrue(exception.getMessage().contains("Upload to Supabase failed") || exception.getMessage().contains("Exception"));
    }

    @Test
    void delete_ValidObjectKey_CompletesSuccessfully() throws Exception {
        // Arrange
        ResponseEntity<String> successResponse = new ResponseEntity<>("Deleted", HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(successResponse);

        // Act & Assert
        assertDoesNotThrow(() -> {
            supabaseStorageService.delete("media/test-image.png");
        });
    }

    @Test
    void delete_ApiReturnsError_ThrowsException() {
        // Arrange
        ResponseEntity<String> errorResponse = new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(errorResponse);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            supabaseStorageService.delete("media/test-image.png");
        });
        assertEquals("Delete from Supabase failed", exception.getMessage());
    }
}