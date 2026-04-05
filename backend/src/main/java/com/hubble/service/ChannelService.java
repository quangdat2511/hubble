package com.hubble.service;

import com.hubble.dto.request.CreateChannelRequest;
import com.hubble.dto.request.UpdateChannelRequest;
import com.hubble.dto.response.ChannelEvent;
import com.hubble.dto.response.ChannelMemberResponse;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ChannelRoleResponse;
import com.hubble.entity.Channel;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.ChannelRole;
import com.hubble.entity.Role;
import com.hubble.entity.Server;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ChannelMapper;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.RoleRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChannelService {
    ChannelMapper channelMapper;
    UserRepository userRepository;
    ChannelRepository channelRepository;
    ChannelMemberRepository channelMemberRepository;
    MessageRepository messageRepository;
    ChannelRoleRepository channelRoleRepository;
    ServerMemberRepository serverMemberRepository;
    ServerRepository serverRepository;
    RoleRepository roleRepository;
    MessageService messageService;
    SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChannelResponse getOrCreateDirectChannel(UUID currentUserId, UUID otherUserId){
        if (currentUserId.equals(otherUserId)) {
            throw new AppException(ErrorCode.CANNOT_DM_SELF);
        }
        if (!userRepository.existsById(otherUserId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        List<ChannelMember> currentUserMembers =
                channelMemberRepository.findAllByUserId(currentUserId);
        List<ChannelMember> otherUserMembers =
                channelMemberRepository.findAllByUserId(otherUserId);

        Set<UUID> currentUserChannelIds = currentUserMembers.stream()
                .map(ChannelMember::getChannelId)
                .collect(Collectors.toSet());

        Set<UUID> otherUserChannelIds = otherUserMembers.stream()
                .map(ChannelMember::getChannelId)
                .collect(Collectors.toSet());

        // 4. Lấy giao nhau
        currentUserChannelIds.retainAll(otherUserChannelIds); // giờ trong set là các channel chung
                // 5. Tìm channel DM trong các channel chung, ưu tiên channel mới nhất để ổn định khi dữ liệu cũ bị trùng.
                List<Channel> sharedChannels = new ArrayList<>(channelRepository.findAllById(currentUserChannelIds));
                sharedChannels.sort(Comparator.comparing(Channel::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

                for (Channel channel : sharedChannels) {
                        UUID channelId = channel.getId();
                        List<ChannelMember> members = channelMemberRepository.findAllByChannelId(channelId);
                        if (channel.getType() == ChannelType.DM
                                        && channel.getServerId() == null
                                        && members.size() == 2) {
                                return buildDirectChannelResponse(channel, currentUserId); // đã có DM 1-1, dùng lại
                        }
                }
        Channel newChannel = Channel.builder()
                .type(ChannelType.DM)
                .serverId(null)
                .name(null)
                .isPrivate(true)
                .build();
        Channel saved = channelRepository.save(newChannel);

        ChannelMember member1 = ChannelMember.builder()
                .channelId(saved.getId())
                .userId(currentUserId)
                .build();

        ChannelMember member2 = ChannelMember.builder()
                .channelId(saved.getId())
                .userId(otherUserId)
                .build();

        channelMemberRepository.saveAll(List.of(member1, member2));

                return buildDirectChannelResponse(saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> getDirectChannels(UUID currentUserId) {
        List<ChannelMember> memberships =
                channelMemberRepository.findAllByUserId(currentUserId);

        if (memberships.isEmpty()) {
            return List.of();
        }

        Set<UUID> channelIds = memberships.stream()
                .map(ChannelMember::getChannelId)
                .collect(Collectors.toSet());

                List<Channel> channels = new ArrayList<>(channelRepository.findAllById(channelIds));

                // Sắp xếp mới -> cũ trước, sau đó gộp theo peer để mỗi người bạn chỉ còn 1 DM.
                channels.sort(Comparator.comparing(Channel::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

                Map<String, ChannelResponse> dedupByPeer = new LinkedHashMap<>();
                for (Channel channel : channels) {
                        if (channel.getType() != ChannelType.DM || channel.getServerId() != null) {
                                continue;
                        }

                        ChannelResponse response = buildDirectChannelResponse(channel, currentUserId);
                        String key = response.getPeerUserId() != null
                                        ? response.getPeerUserId()
                                        : "channel:" + response.getId();
                        dedupByPeer.putIfAbsent(key, response);
                }

                return new ArrayList<>(dedupByPeer.values());
    }

        private ChannelResponse buildDirectChannelResponse(Channel channel, UUID currentUserId) {
                ChannelResponse response = channelMapper.toChannelResponse(channel);
                fillUnreadCount(response, channel.getId(), currentUserId);

                Optional<ChannelMember> peerMember = channelMemberRepository.findAllByChannelId(channel.getId())
                                .stream()
                                .filter(member -> !member.getUserId().equals(currentUserId))
                                .findFirst();

                if (peerMember.isEmpty()) {
                        return response;
                }

                userRepository.findById(peerMember.get().getUserId()).ifPresent(peerUser -> fillPeer(response, peerUser));
                return response;
        }

        private void fillUnreadCount(ChannelResponse response, UUID channelId, UUID currentUserId) {
                channelMemberRepository.findByChannelIdAndUserId(channelId, currentUserId)
                                .ifPresentOrElse(
                                                member -> {
                                                        long c;
                                                        if (member.getLastReadAt() == null) {
                                                                c = messageRepository.countIncomingMessagesFromOthers(
                                                                                channelId, currentUserId);
                                                        } else {
                                                                c = messageRepository.countIncomingMessagesAfterRead(
                                                                                channelId, currentUserId,
                                                                                member.getLastReadAt());
                                                        }
                                                        response.setUnreadCount((int) Math.min(c, 999));
                                                },
                                                () -> response.setUnreadCount(0)
                                );
        }

        private void fillPeer(ChannelResponse response, User peerUser) {
                response.setPeerUserId(peerUser.getId().toString());
                response.setPeerUsername(peerUser.getUsername());
                response.setPeerDisplayName(peerUser.getDisplayName());
                response.setPeerAvatarUrl(peerUser.getAvatarUrl());
                response.setPeerStatus(peerUser.getStatus() != null ? peerUser.getStatus().name() : null);
        }
    @Transactional
    public ChannelResponse createChannel(UUID serverId, UUID creatorUserId, CreateChannelRequest request) {
        // Calculate next position
        List<Channel> existing = channelRepository.findByServerId(serverId);
        short maxPos = existing.stream()
                .filter(c -> java.util.Objects.equals(c.getParentId(), request.getParentId()))
                .map(Channel::getPosition)
                .max(Short::compare)
                .orElse((short) -1);

        Channel channel = Channel.builder()
                .serverId(serverId)
                .parentId(request.getParentId())
                .name(request.getName())
                .type(request.getType())
                .isPrivate(request.getIsPrivate() != null && request.getIsPrivate())
                .position((short) (maxPos + 1))
                .build();
        channel = channelRepository.save(channel);

        // If private, always add creator + any selected members/roles
        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
            UUID channelId = channel.getId();

            // Build member set: always include creator
            Set<UUID> memberSet = new java.util.LinkedHashSet<>();
            memberSet.add(creatorUserId);
            if (request.getMemberIds() != null) {
                memberSet.addAll(request.getMemberIds());
            }
            List<ChannelMember> members = memberSet.stream()
                    .map(userId -> ChannelMember.builder()
                            .channelId(channelId)
                            .userId(userId)
                            .build())
                    .toList();
            channelMemberRepository.saveAll(members);

            if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                List<ChannelRole> roles = request.getRoleIds().stream()
                        .map(roleId -> ChannelRole.builder()
                                .channelId(channelId)
                                .roleId(roleId)
                                .build())
                        .toList();
                channelRoleRepository.saveAll(roles);
            }
        }

        ChannelResponse response = channelMapper.toChannelResponse(channel);
        broadcastChannelEvent(serverId, "CREATED", response);
        return response;
    }

    private void broadcastChannelEvent(UUID serverId, String type, ChannelResponse response) {
        ChannelEvent event = ChannelEvent.builder()
                .type(type)
                .channel(response)
                .build();
        messagingTemplate.convertAndSend("/topic/servers/" + serverId + "/channels", event);
    }

    @Transactional
    public ChannelResponse updateChannel(UUID channelId, UpdateChannelRequest request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        if (request.getName() != null) channel.setName(request.getName());
        if (request.getTopic() != null) channel.setTopic(request.getTopic());
        if (request.getParentId() != null) channel.setParentId(request.getParentId());
        if (request.getIsPrivate() != null) channel.setIsPrivate(request.getIsPrivate());

        channel = channelRepository.save(channel);
        ChannelResponse response = channelMapper.toChannelResponse(channel);
        broadcastChannelEvent(channel.getServerId(), "UPDATED", response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getChannelMembers(UUID channelId) {
        channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        Channel channel = channelRepository.findById(channelId).get();
        UUID ownerId = null;
        if (channel.getServerId() != null) {
            ownerId = serverRepository.findById(channel.getServerId())
                    .map(Server::getOwnerId).orElse(null);
        }

        UUID finalOwnerId = ownerId;
        return channelMemberRepository.findAllByChannelId(channelId).stream()
                .map(cm -> {
                    User user = userRepository.findById(cm.getUserId()).orElse(null);
                    if (user == null) return null;
                    return ChannelMemberResponse.builder()
                            .userId(user.getId().toString())
                            .username(user.getUsername())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .isOwner(user.getId().equals(finalOwnerId))
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelRoleResponse> getChannelRoles(UUID channelId) {
        channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        return channelRoleRepository.findAllByChannelId(channelId).stream()
                .map(cr -> {
                    Role role = roleRepository.findById(cr.getRoleId()).orElse(null);
                    if (role == null) return null;
                    return ChannelRoleResponse.builder()
                            .roleId(role.getId().toString())
                            .name(role.getName())
                            .color(role.getColor())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public void addChannelMembers(UUID channelId, List<UUID> userIds) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        List<ChannelMember> members = userIds.stream()
                .filter(uid -> !channelMemberRepository.existsByChannelIdAndUserId(channelId, uid))
                .map(uid -> ChannelMember.builder().channelId(channelId).userId(uid).build())
                .toList();
        channelMemberRepository.saveAll(members);
        broadcastChannelEvent(channel.getServerId(), "UPDATED", channelMapper.toChannelResponse(channel));
    }

    @Transactional
    public void addChannelRoles(UUID channelId, List<UUID> roleIds) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        List<ChannelRole> existing = channelRoleRepository.findAllByChannelId(channelId);
        Set<UUID> existingIds = existing.stream().map(ChannelRole::getRoleId).collect(Collectors.toSet());

        List<ChannelRole> roles = roleIds.stream()
                .filter(rid -> !existingIds.contains(rid))
                .map(rid -> ChannelRole.builder().channelId(channelId).roleId(rid).build())
                .toList();
        channelRoleRepository.saveAll(roles);
        broadcastChannelEvent(channel.getServerId(), "UPDATED", channelMapper.toChannelResponse(channel));
    }

    @Transactional
    public void removeChannelMember(UUID channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        channelMemberRepository.deleteByChannelIdAndUserId(channelId, userId);
        broadcastChannelEvent(channel.getServerId(), "UPDATED", channelMapper.toChannelResponse(channel));
    }

    @Transactional
    public void removeChannelRole(UUID channelId, UUID roleId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        channelRoleRepository.deleteByChannelIdAndRoleId(channelId, roleId);
        broadcastChannelEvent(channel.getServerId(), "UPDATED", channelMapper.toChannelResponse(channel));
    }

    @Transactional
    public void deleteChannel(UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        UUID serverId = channel.getServerId();
        ChannelResponse response = channelMapper.toChannelResponse(channel);

        channelMemberRepository.deleteAllByChannelId(channelId);
        channelRoleRepository.deleteAllByChannelId(channelId);
        messageService.deleteMessagesByChannel(channelId);
        channelRepository.deleteById(channelId);

        broadcastChannelEvent(serverId, "DELETED", response);
    }
}
