package com.hubble.service;

import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ServerResponse;
import com.hubble.entity.Channel;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.ChannelRepository;
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
    ChannelRepository channelRepository;

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

    public List<ChannelResponse> getServerChannels(UUID serverId) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        return channelRepository.findByServerId(serverId)
                .stream()
                .map(channel -> ChannelResponse.builder()
                        .id(channel.getId().toString())
                        .serverId(channel.getServerId().toString())
                        .parentId(channel.getParentId() != null ? channel.getParentId().toString() : null)
                        .name(channel.getName())
                        .type(channel.getType().toString())
                        .topic(channel.getTopic())
                        .position(channel.getPosition())
                        .isPrivate(channel.getIsPrivate())
                        .createdAt(channel.getCreatedAt() != null ? channel.getCreatedAt().toString() : null)
                        .build())
                .toList();
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

        createDefaultChannels(server.getId());

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

    private void createDefaultChannels(UUID serverId) {
        Channel chatCategory = Channel.builder()
                .serverId(serverId)
                .name("Kênh chat")
                .type(ChannelType.CATEGORY)
                .position((short) 0)
                .isPrivate(false)
                .build();
        chatCategory = channelRepository.save(chatCategory);

        channelRepository.save(Channel.builder()
                .serverId(serverId)
                .parentId(chatCategory.getId())
                .name("Kênh chung")
                .type(ChannelType.TEXT)
                .position((short) 0)
                .isPrivate(false)
                .build());

        Channel voiceCategory = Channel.builder()
                .serverId(serverId)
                .name("Kênh đàm thoại")
                .type(ChannelType.CATEGORY)
                .position((short) 1)
                .isPrivate(false)
                .build();
        voiceCategory = channelRepository.save(voiceCategory);

        channelRepository.save(Channel.builder()
                .serverId(serverId)
                .parentId(voiceCategory.getId())
                .name("Chung")
                .type(ChannelType.VOICE)
                .position((short) 0)
                .isPrivate(false)
                .build());
    }
}
