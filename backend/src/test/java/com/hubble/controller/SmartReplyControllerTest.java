package com.hubble.controller;

import com.hubble.dto.response.SmartReplyResponse;
import com.hubble.service.SmartReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartReplyControllerTest {

    @Mock
    private SmartReplyService smartReplyService;

    @InjectMocks
    private SmartReplyController smartReplyController;

    private Map<String, String> validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new HashMap<>();
        validRequest.put("content", "Xin chào");
    }

    @Test
    void getSmartReply_ValidContent_ReturnsOk() {
        // Arrange
        SmartReplyResponse mockResponse = new SmartReplyResponse();
        when(smartReplyService.generateSuggestions("Xin chào")).thenReturn(mockResponse);

        // Act
        ResponseEntity<SmartReplyResponse> responseEntity = smartReplyController.getSmartReply(validRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Test
    void getSmartReply_NullContent_ReturnsBadRequest() {
        // Arrange
        validRequest.put("content", null);
        when(smartReplyService.generateSuggestions(null)).thenReturn(null);

        // Act
        ResponseEntity<SmartReplyResponse> responseEntity = smartReplyController.getSmartReply(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getSmartReply_ServiceReturnsNull_ReturnsBadRequest() {
        // Arrange
        when(smartReplyService.generateSuggestions(anyString())).thenReturn(null);

        // Act
        ResponseEntity<SmartReplyResponse> responseEntity = smartReplyController.getSmartReply(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
}