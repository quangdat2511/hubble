package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    MessageMapper messageMapper;
    ChannelRepository channelRepository;
    SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String channelId, int page, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        if (!channelRepository.existsById(channelUuid)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        return messageRepository
                .findByChannelIdOrderByCreatedAtDesc(channelUuid, PageRequest.of(page, size))
                .map(messageMapper::toMessageResponse)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(String authorId, CreateMessageRequest request) {
        UUID channelUuid = UUID.fromString(request.getChannelId());
        if (!channelRepository.existsById(channelUuid)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }

        Message message = Message.builder()
                .channelId(channelUuid)
                .authorId(UUID.fromString(authorId))
                .replyToId(request.getReplyToId() != null ? UUID.fromString(request.getReplyToId()) : null)
                .content(request.getContent())
                .build();

        Message saved = messageRepository.save(message);
        MessageResponse response = messageMapper.toMessageResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/channels/" + request.getChannelId(),
                response
        );

        return response;
    }

    @Transactional
    public void deleteMessagesByChannel(UUID channelId) {
        messageRepository.deleteAllByChannelId(channelId);
    }
}
