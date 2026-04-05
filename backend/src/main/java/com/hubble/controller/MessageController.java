package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.MarkChannelReadRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.response.PeerReadStatusResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.ChannelReadService;
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
    ChannelReadService channelReadService;

    @PostMapping("/channel/{channelId}/read")
    public ApiResponse<Void> markChannelRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestBody MarkChannelReadRequest request
    ) {
        channelReadService.markRead(principal.getId(), channelId, request);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/channel/{channelId}/peer-read-status")
    public ApiResponse<PeerReadStatusResponse> getPeerReadStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId
    ) {
        return ApiResponse.<PeerReadStatusResponse>builder()
                .result(channelReadService.getPeerReadStatus(principal.getId(), channelId))
                .build();
    }

    @GetMapping("/{channelId}")
    public ApiResponse<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.<List<MessageResponse>>builder()
                .result(messageService.getMessages(channelId, principal.getId().toString(), page, size))
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
