package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.AttachmentResponse;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.AttachmentRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    AttachmentRepository attachmentRepository;   // ← thêm từ branch
    MessageMapper messageMapper;
    ChannelRepository channelRepository;
    SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String channelId, int page, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        if (!channelRepository.existsById(channelUuid)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }

        List<Message> messages = messageRepository
                .findByChannelIdOrderByCreatedAtDesc(channelUuid, PageRequest.of(page, size))
                .toList();

        if (messages.isEmpty()) return List.of();

        // Batch load attachments — no N+1
        List<UUID> messageIds = messages.stream()
                .map(Message::getId)
                .toList();

        Map<UUID, List<AttachmentResponse>> attachmentsByMessageId = attachmentRepository
                .findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Attachment::getMessageId,
                        Collectors.mapping(
                                a -> AttachmentResponse.builder()
                                        .id(a.getId())
                                        .filename(a.getFilename())
                                        .url(a.getUrl())
                                        .contentType(a.getContentType())
                                        .sizeBytes(a.getSizeBytes())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        return messages.stream()
                .map(msg -> messageMapper.toMessageResponse(
                        msg,
                        attachmentsByMessageId.getOrDefault(msg.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(String authorId, CreateMessageRequest request) {
        UUID channelUuid = request.getChannelId();  // ← bỏ UUID.fromString()
        if (!channelRepository.existsById(channelUuid)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }

        Message message = Message.builder()
                .channelId(request.getChannelId())                    // ← trực tiếp
                .authorId(UUID.fromString(authorId))                  // ← authorId vẫn là String nên giữ
                .replyToId(request.getReplyToId())                    // ← bỏ UUID.fromString() + null check
                .content(request.getContent())
                .build();

        Message saved = messageRepository.save(message);

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            List<Attachment> attachments = attachmentRepository.findAllById(request.getAttachmentIds());
            attachments.forEach(a -> a.setMessageId(saved.getId()));
            attachmentRepository.saveAll(attachments);
        }

        MessageResponse response = buildResponse(saved);

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