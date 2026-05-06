package com.hubble.service;

import com.hubble.dto.request.ServerInviteRequest;
import com.hubble.dto.response.ServerInviteResponse;
import com.hubble.entity.ServerInvite;
import com.hubble.entity.ServerMember;
import com.hubble.entity.User;
import com.hubble.entity.Server;
import com.hubble.enums.NotificationType;
import com.hubble.enums.Permission;
import com.hubble.enums.ServerInviteStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ServerInviteMapper;
import com.hubble.repository.ServerInviteRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerInviteService {

    ServerRepository serverRepository;
    ServerInviteRepository serverInviteRepository;
    ServerMemberRepository serverMemberRepository;
    UserRepository userRepository;
    ServerInviteMapper serverInviteMapper;
    NotificationService notificationService;
    RoleService roleService;

    @Transactional
    public ServerInviteResponse inviteUser(UUID inviterId, UUID serverId, ServerInviteRequest request) {
        User invitee = userRepository.findByUsername(request.getInviteeUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UUID inviteeId = invitee.getId();

        if (inviterId.equals(inviteeId)) {
            throw new AppException(ErrorCode.CANNOT_INVITE_SELF);
        }

        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!server.getOwnerId().equals(inviterId)
                && !roleService.hasServerPermission(serverId, inviterId, Permission.INVITE_MEMBERS)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (serverMemberRepository.existsByServerIdAndUserId(serverId, inviteeId)) {
            throw new AppException(ErrorCode.ALREADY_SERVER_MEMBER);
        }

        if (serverInviteRepository.existsByServerIdAndInviteeIdAndStatus(serverId, inviteeId, ServerInviteStatus.PENDING)) {
            throw new AppException(ErrorCode.SERVER_INVITE_ALREADY_SENT);
        }

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ServerInvite invite = serverInviteRepository.save(
                ServerInvite.builder()
                        .serverId(serverId)
                        .inviterId(inviterId)
                        .inviteeId(inviteeId)
                        .status(ServerInviteStatus.PENDING)
                        .build()
        );

        ServerInviteResponse response = serverInviteMapper.toResponse(invite, server, inviter, invitee);

        String inviterName = inviter.getDisplayName() != null ? inviter.getDisplayName() : inviter.getUsername();
        notificationService.dispatchNotification(
                inviteeId,
                NotificationType.SERVER_INVITE,
                serverId.toString(),
                inviterName + " đã mời bạn tham gia " + server.getName() + ".",
                false,
                true
        );

        return response;
    }

    @Transactional
    public void acceptInvite(UUID userId, UUID inviteId) {
        ServerInvite invite = serverInviteRepository.findById(inviteId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_INVITE_NOT_FOUND));

        if (!invite.getInviteeId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (invite.getStatus() != ServerInviteStatus.PENDING) {
            throw new AppException(ErrorCode.SERVER_INVITE_ALREADY_RESPONDED);
        }

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.SERVER_INVITE_EXPIRED);
        }

        if (serverMemberRepository.existsByServerIdAndUserId(invite.getServerId(), userId)) {
            throw new AppException(ErrorCode.ALREADY_SERVER_MEMBER);
        }

        invite.setStatus(ServerInviteStatus.ACCEPTED);
        invite.setRespondedAt(LocalDateTime.now());
        serverInviteRepository.save(invite);

        serverMemberRepository.save(
                ServerMember.builder()
                        .serverId(invite.getServerId())
                        .userId(userId)
                        .build()
        );
    }

    @Transactional
    public void declineInvite(UUID userId, UUID inviteId) {
        ServerInvite invite = serverInviteRepository.findById(inviteId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_INVITE_NOT_FOUND));

        if (!invite.getInviteeId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (invite.getStatus() != ServerInviteStatus.PENDING) {
            throw new AppException(ErrorCode.SERVER_INVITE_ALREADY_RESPONDED);
        }

        invite.setStatus(ServerInviteStatus.DECLINED);
        invite.setRespondedAt(LocalDateTime.now());
        serverInviteRepository.save(invite);
    }

    public List<ServerInviteResponse> getPendingInvitesByServer(UUID requestorId, UUID serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!server.getOwnerId().equals(requestorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User inviter = userRepository.findById(requestorId).orElse(null);

        return serverInviteRepository.findAllByServerIdAndStatus(serverId, ServerInviteStatus.PENDING)
                .stream()
                .map(invite -> {
                    User invitee = userRepository.findById(invite.getInviteeId()).orElse(null);
                    return serverInviteMapper.toResponse(invite, server, inviter, invitee);
                })
                .toList();
    }

    public List<ServerInviteResponse> getMyPendingInvites(UUID userId) {
        User invitee = userRepository.findById(userId).orElse(null);

        return serverInviteRepository.findAllByInviteeIdAndStatus(userId, ServerInviteStatus.PENDING)
                .stream()
                .map(invite -> {
                    Server server = serverRepository.findById(invite.getServerId()).orElse(null);
                    User inviter = userRepository.findById(invite.getInviterId()).orElse(null);
                    return serverInviteMapper.toResponse(invite, server, inviter, invitee);
                })
                .toList();
    }
}

