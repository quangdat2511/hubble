package com.hubble.service;

import com.hubble.dto.request.CreateChannelRequest;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.entity.Channel;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.ChannelRole;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ChannelMapper;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.ServerMemberRepository;
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
        broadcastChannelCreated(serverId, response);
        return response;
    }

    private void broadcastChannelCreated(UUID serverId, ChannelResponse response) {
        messagingTemplate.convertAndSend("/topic/servers/" + serverId + "/channels", response);
    }

    @Transactional
    public void deleteChannel(UUID channelId) {
        messageService.deleteMessagesByChannel(channelId);
        channelRepository.deleteById(channelId);
    }
}
