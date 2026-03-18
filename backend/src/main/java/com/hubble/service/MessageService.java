package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.event.MessageEvent;
import com.hubble.dto.request.EditMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    MessageMapper messageMapper;
    SimpMessagingTemplate messagingTemplate;
    ChannelRepository channelRepository;
    ChannelMemberRepository channelMemberRepository;

    public MessageResponse sendMessage(UUID currentUserId, CreateMessageRequest request) {
        UUID channelId = request.getChannelId();

        // 1. Kiểm tra channel tồn tại
        channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        // 2. Kiểm tra user là member của channel
        boolean isMember = channelMemberRepository.existsByChannelIdAndUserId(channelId, currentUserId);
        if (!isMember) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        };

        // 4. Lưu & broadcast như cũ
        Message newMessage = messageMapper.toMessage(request, currentUserId);

        Message savedMessage = messageRepository.save(newMessage);
        MessageResponse response = messageMapper.toMessageResponse(savedMessage);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + savedMessage.getChannelId(),
                MessageEvent.builder().action("SEND").message(response).build()
        );
        return response;
    }
    @Transactional
    public MessageResponse editMessage(UUID messageId, EditMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        MessageResponse response = messageMapper.toMessageResponse(saved);
        messagingTemplate.convertAndSend(
                "/topic/channel/" + saved.getChannelId(),
                MessageEvent.builder().action("EDIT").message(response).build()
        );
        return response;
    }
    @Transactional
    public void deleteMessage(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        message.setIsDeleted(true);
        messageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + message.getChannelId(),
                MessageEvent.builder()
                        .action("DELETE")
                        .message(MessageResponse.builder().id(messageId).build())
                        .build()
        );
    }
    public List<MessageResponse> getMessagesByChannel(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }
}