package com.hubble.controller;

import com.hubble.configuration.properties.AgoraProperties;
import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.AgoraTokenResponse;
import com.hubble.dto.response.VoiceParticipant;
import com.hubble.entity.User;
import com.hubble.repository.UserRepository;
import com.hubble.security.UserPrincipal;
import com.hubble.service.AgoraUidMapper;
import com.hubble.service.VoiceStateManager;
import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Issues short-lived Agora RTC tokens for voice channel participants.
 * Endpoint: POST /api/voice/{channelId}/agora-token
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AgoraTokenController {

    AgoraProperties agoraProperties;
    AgoraUidMapper agoraUidMapper;
    VoiceStateManager voiceStateManager;
    UserRepository userRepository;

    @PostMapping("/api/voice/{channelId}/agora-token")
    public ApiResponse<AgoraTokenResponse> getAgoraToken(@PathVariable String channelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID currentUserId = principal.getId();

        int uid = agoraUidMapper.toAgoraUid(currentUserId);
        int expirySeconds = agoraProperties.getExpirySeconds();

        RtcTokenBuilder2 builder = new RtcTokenBuilder2();
        String token = builder.buildTokenWithUid(
                agoraProperties.getAppId(),
                agoraProperties.getAppCertificate(),
                channelId,
                uid,
                Role.ROLE_PUBLISHER,
                expirySeconds,
                expirySeconds
        );

        // Register user as active participant
        User user = userRepository.findById(currentUserId).orElse(null);
        if (user != null) {
            VoiceParticipant participant = VoiceParticipant.builder()
                    .userId(currentUserId.toString())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
            voiceStateManager.addParticipant(channelId, participant);
        }

        log.info("[AGORA] Token issued: userId={} channelId={} uid={}", currentUserId, channelId, uid);

        AgoraTokenResponse response = AgoraTokenResponse.builder()
                .token(token)
                .uid(uid)
                .expiresIn(expirySeconds)
                .appId(agoraProperties.getAppId())
                .build();

        return ApiResponse.<AgoraTokenResponse>builder()
                .result(response)
                .build();
    }
}
