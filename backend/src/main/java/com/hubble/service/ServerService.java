package com.hubble.service;

import com.hubble.dto.response.ServerResponse;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerService {

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
}
