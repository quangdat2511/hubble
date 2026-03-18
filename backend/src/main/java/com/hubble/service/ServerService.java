package com.hubble.service;

import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.response.ServerResponse;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerService {

    static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    static final int INVITE_CODE_LENGTH = 10;
    static final SecureRandom RANDOM = new SecureRandom();

    ServerRepository serverRepository;
    ServerMemberRepository serverMemberRepository;

    public ServerResponse getServer(UUID serverId) {
        var server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        return ServerResponse.builder()
                .id(server.getId())
                .ownerId(server.getOwnerId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .inviteCode(server.getInviteCode())
                .isPublic(server.getIsPublic())
                .createdAt(server.getCreatedAt())
                .build();
    }

    public List<ServerResponse> getMyServers(UUID userId) {
        List<UUID> serverIds = serverMemberRepository.findAllByUserId(userId)
                .stream()
                .map(member -> member.getServerId())
                .toList();

        return serverRepository.findAllById(serverIds)
                .stream()
                .map(server -> ServerResponse.builder()
                        .id(server.getId())
                        .ownerId(server.getOwnerId())
                        .name(server.getName())
                        .description(server.getDescription())
                        .iconUrl(server.getIconUrl())
                        .inviteCode(server.getInviteCode())
                        .isPublic(server.getIsPublic())
                        .createdAt(server.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public ServerResponse createServer(UUID userId, CreateServerRequest request) {
        String inviteCode = generateUniqueInviteCode();

        Server server = Server.builder()
                .ownerId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .inviteCode(inviteCode)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        server = serverRepository.save(server);

        ServerMember ownerMember = ServerMember.builder()
                .serverId(server.getId())
                .userId(userId)
                .build();

        serverMemberRepository.save(ownerMember);

        return ServerResponse.builder()
                .id(server.getId())
                .ownerId(server.getOwnerId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .inviteCode(server.getInviteCode())
                .isPublic(server.getIsPublic())
                .createdAt(server.getCreatedAt())
                .build();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (serverRepository.existsByInviteCode(code));
        return code;
    }
}
