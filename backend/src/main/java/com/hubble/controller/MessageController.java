package com.hubble.controller;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.EditMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.common.ApiResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.MessageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageController {

    MessageService messageService;

    // Gửi & nhận tin nhắn realtime
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@RequestBody CreateMessageRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID currentUserId = principal.getId();

        MessageResponse messageResponse = messageService.sendMessage(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                .result(messageResponse)
                .build());
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<MessageResponse> messages = messageService.getMessagesByChannel(channelId, page, size);
        return ResponseEntity.ok(ApiResponse.<List<MessageResponse>>builder()
                .result(messages)
                .build());
    }
    @PatchMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable UUID messageId,
            @RequestBody @Valid EditMessageRequest request
    ) {
        MessageResponse response = messageService.editMessage(messageId, request);
        return ResponseEntity.ok(ApiResponse.<MessageResponse>builder().result(response).build());
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}
