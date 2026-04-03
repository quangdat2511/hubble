package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.response.SharedContentPageResponse;
import com.hubble.enums.SharedContentType;
import com.hubble.security.UserPrincipal;
import com.hubble.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageController {

    MessageService messageService;

    @GetMapping("/{channelId}")
    public ApiResponse<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.<List<MessageResponse>>builder()
                .result(messageService.getMessages(principal.getId().toString(), channelId, page, size))
                .build();
    }

    @GetMapping("/{channelId}/shared-content")
    public ApiResponse<SharedContentPageResponse> getSharedContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "MEDIA") SharedContentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return ApiResponse.<SharedContentPageResponse>builder()
                .result(messageService.getSharedContent(principal.getId().toString(), channelId, type, page, size))
                .build();
    }

    @PostMapping
    public ApiResponse<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateMessageRequest request
    ) {
        return ApiResponse.<MessageResponse>builder()
                .result(messageService.sendMessage(principal.getId().toString(), request))
                .build();
    }

    @PatchMapping("/{messageId}")
    public ApiResponse<MessageResponse> editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String messageId,
            @RequestBody UpdateMessageRequest request
    ) {
        return ApiResponse.<MessageResponse>builder()
                .result(messageService.editMessage(principal.getId().toString(), messageId, request))
                .build();
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<MessageResponse> unsendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String messageId
    ) {
        return ApiResponse.<MessageResponse>builder()
                .result(messageService.unsendMessage(principal.getId().toString(), messageId))
                .build();
    }
}
