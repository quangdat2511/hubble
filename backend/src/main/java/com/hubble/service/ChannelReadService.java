package com.hubble.service;

import com.hubble.dto.request.MarkChannelReadRequest;
import com.hubble.dto.response.PeerReadStatusResponse;
import com.hubble.dto.response.ReadReceiptEvent;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChannelReadService {

    ChannelMemberRepository channelMemberRepository;
    ChannelRepository channelRepository;
    MessageRepository messageRepository;
    SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void markRead(UUID userId, String channelIdStr, MarkChannelReadRequest request) {
        if (request == null || request.getMessageId() == null || request.getMessageId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        UUID channelId = UUID.fromString(channelIdStr);
        UUID messageId = UUID.fromString(request.getMessageId());

        if (!channelRepository.existsById(channelId)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }

        ChannelMember self = channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        if (!message.getChannelId().equals(channelId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        LocalDateTime boundary = message.getCreatedAt();
        LocalDateTime existing = self.getLastReadAt();
        boolean advanced = existing == null || boundary.isAfter(existing);
        if (advanced) {
            self.setLastReadAt(boundary);
            channelMemberRepository.save(self);
            ReadReceiptEvent event = ReadReceiptEvent.builder()
                    .userId(userId.toString())
                    .lastReadMessageId(messageId.toString())
                    .readAt(boundary.toString())
                    .build();
            messagingTemplate.convertAndSend("/topic/channels/" + channelIdStr + "/read", event);
        }
    }

    @Transactional(readOnly = true)
    public PeerReadStatusResponse getPeerReadStatus(UUID currentUserId, String channelIdStr) {
        UUID channelId = UUID.fromString(channelIdStr);
        if (!channelRepository.existsById(channelId)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        List<ChannelMember> members = channelMemberRepository.findAllByChannelId(channelId);
        UUID peerUserId = null;
        for (ChannelMember m : members) {
            if (m != null && m.getUserId() != null && !m.getUserId().equals(currentUserId)) {
                peerUserId = m.getUserId();
                break;
            }
        }
        if (peerUserId == null) {
            return PeerReadStatusResponse.builder().readAt(null).build();
        }

        return channelMemberRepository.findByChannelIdAndUserId(channelId, peerUserId)
                .map(p -> {
                    LocalDateTime lr = p.getLastReadAt();
                    return PeerReadStatusResponse.builder()
                            .readAt(lr != null ? lr.toString() : null)
                            .build();
                })
                .orElse(PeerReadStatusResponse.builder().readAt(null).build());
    }
}
