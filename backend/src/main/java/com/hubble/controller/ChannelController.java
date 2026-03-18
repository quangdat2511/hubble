package com.hubble.controller;

import com.hubble.dto.response.ChannelResponse;
import com.hubble.entity.Channel;
import com.hubble.security.UserPrincipal;
import com.hubble.service.ChannelService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChannelController {

    ChannelService channelService;

    @PostMapping("/dm/{otherUserId}")
    public ChannelResponse getOrCreateDirectChannel(@PathVariable UUID otherUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID currentUserId = principal.getId();

        return channelService.getOrCreateDirectChannel(currentUserId, otherUserId);
    }
    @GetMapping("/dm")
    public List<ChannelResponse> getDirectChannels() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID currentUserId = principal.getId();

        return channelService.getDirectChannels(currentUserId);
    }
}
