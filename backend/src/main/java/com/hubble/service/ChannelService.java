package com.hubble.service;

import com.hubble.dto.response.ChannelResponse;
import com.hubble.entity.Channel;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ChannelMapper;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        // 5. Tìm channel DM trong các channel chung
        for (UUID channelId : currentUserChannelIds) {
            Channel channel = channelRepository.findById(channelId).orElse(null);
                        List<ChannelMember> members = channelMemberRepository.findAllByChannelId(channelId);
            if (channel != null
                    && channel.getType() == ChannelType.DM
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

        List<Channel> channels = channelRepository.findAllById(channelIds);

        return channels.stream()
                .filter(c -> c.getType() == ChannelType.DM && c.getServerId() == null)
                                .map(channel -> buildDirectChannelResponse(channel, currentUserId))
                .toList();
    }

        private ChannelResponse buildDirectChannelResponse(Channel channel, UUID currentUserId) {
                ChannelResponse response = channelMapper.toChannelResponse(channel);

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

        private void fillPeer(ChannelResponse response, User peerUser) {
                response.setPeerUserId(peerUser.getId().toString());
                response.setPeerUsername(peerUser.getUsername());
                response.setPeerDisplayName(peerUser.getDisplayName());
                response.setPeerAvatarUrl(peerUser.getAvatarUrl());
                response.setPeerStatus(peerUser.getStatus() != null ? peerUser.getStatus().name() : null);
        }
}
