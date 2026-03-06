package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    MessageMapper messageMapper;
    SimpMessagingTemplate messagingTemplate;

    public MessageResponse sendMessage(CreateMessageRequest request) {
        Message newMessage = messageMapper.toMessage(request);
        Message savedMessage = messageRepository.save(newMessage);
        MessageResponse response = toMessageResponse(savedMessage);
        String destination = "/topic/channel/" + savedMessage.getChannelId();
        messagingTemplate.convertAndSend(destination, response);
        return response;
    }

    public List<MessageResponse> getMessagesByChannel(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .channelId(message.getChannelId())
                .authorId(message.getAuthorId())
                .replyToId(message.getReplyToId())
                .content(message.getContent())
                .type(message.getType())
                .isPinned(message.getIsPinned())
                .editedAt(message.getEditedAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}