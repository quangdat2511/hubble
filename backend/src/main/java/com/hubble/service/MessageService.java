package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.event.MessageEvent;
import com.hubble.dto.request.EditMessageRequest;
import com.hubble.dto.response.AttachmentResponse;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.AttachmentMapper;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.AttachmentRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    AttachmentRepository attachmentRepository;
    MessageMapper messageMapper;
    SimpMessagingTemplate messagingTemplate;
    ChannelRepository channelRepository;
    ChannelMemberRepository channelMemberRepository;
    AttachmentMapper attachmentMapper;          // inject this


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
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            List<Attachment> attachments = attachmentRepository.findAllById(request.getAttachmentIds());
            attachments.forEach(a -> a.setMessageId(savedMessage.getId()));
            attachmentRepository.saveAll(attachments);
        }
        MessageResponse response = buildResponse(savedMessage);

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

        MessageResponse response = buildResponse(saved);
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
//    public List<MessageResponse> getMessagesByChannel(UUID channelId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
//                .stream()
//                .map(messageMapper::toMessageResponse)
//                .toList();
//    }

    public List<MessageResponse> getMessagesByChannel(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<Message> messages = messageRepository
                .findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .stream()
                .toList();

        if (messages.isEmpty()) return List.of();

        // Single batched query — no N+1
        List<UUID> messageIds = messages.stream()
                .map(Message::getId)
                .toList();

        Map<UUID, List<AttachmentResponse>> attachmentsByMessageId = attachmentRepository
                .findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Attachment::getMessageId,
                        Collectors.mapping(attachmentMapper::toAttachmentResponse, Collectors.toList())
                ));

        return messages.stream()
                .map(msg -> messageMapper.toMessageResponse(
                        msg,
                        attachmentsByMessageId.getOrDefault(msg.getId(), List.of())
                ))
                .toList();
    }

    private MessageResponse buildResponse(Message message) {
        List<AttachmentResponse> attachments = attachmentRepository
                .findByMessageId(message.getId())
                .stream()
                .map(a -> AttachmentResponse.builder()
                        .id(a.getId())
                        .filename(a.getFilename())
                        .url(a.getUrl())
                        .contentType(a.getContentType())
                        .sizeBytes(a.getSizeBytes())
                        .build())
                .toList();

        return messageMapper.toMessageResponse(message, attachments);
    }
}