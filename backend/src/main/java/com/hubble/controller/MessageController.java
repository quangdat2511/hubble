package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.MessageResponse;
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
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.<List<MessageResponse>>builder()
                .result(messageService.getMessages(channelId, page, size))
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
}
