package com.hubble.service;

import com.hubble.dto.response.ServerEventNotification;
import com.hubble.dto.response.ServerMemberResponse;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.entity.User;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ServerMemberMapper;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerMemberService {

    ServerRepository serverRepository;
    ServerMemberRepository serverMemberRepository;
    UserRepository userRepository;
    ServerMemberMapper serverMemberMapper;
    SimpMessagingTemplate messagingTemplate;

    public List<ServerMemberResponse> getServerMembers(UUID requestorId, UUID serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, requestorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        List<ServerMember> members = serverMemberRepository.findAllByServerId(serverId);
        List<UUID> userIds = members.stream().map(ServerMember::getUserId).toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return members.stream()
                .map(member -> serverMemberMapper.toResponse(member, userMap.get(member.getUserId()), server))
                .toList();
    }

    @Transactional
    public void kickMember(UUID requestorId, UUID serverId, UUID targetUserId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!server.getOwnerId().equals(requestorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (server.getOwnerId().equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_KICK_OWNER);
        }

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_MEMBER_NOT_FOUND));

        serverMemberRepository.delete(member);

        // Notify the kicked user in real-time so their app reloads the server list
        messagingTemplate.convertAndSend(
                "/topic/users/" + targetUserId + "/server-events",
                ServerEventNotification.builder()
                        .type("KICKED")
                        .serverId(serverId)
                        .serverName(server.getName())
                        .build()
        );
    }

    @Transactional
    public void transferOwnership(UUID requestorId, UUID serverId, UUID newOwnerId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!server.getOwnerId().equals(requestorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, newOwnerId)) {
            throw new AppException(ErrorCode.SERVER_MEMBER_NOT_FOUND);
        }

        server.setOwnerId(newOwnerId);
        serverRepository.save(server);
    }
}
