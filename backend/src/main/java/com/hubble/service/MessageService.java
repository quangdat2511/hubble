package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.AttachmentResponse;
import com.hubble.dto.response.DmDeliveryEvent;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.response.ReactionResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    ChannelRepository channelRepository;
    ChannelMemberRepository channelMemberRepository;
    SimpMessagingTemplate messagingTemplate;
    ReactionService reactionService;

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

        Map<UUID, List<ReactionResponse>> reactionsByMessageId =
                reactionService.getReactionsForMessages(messageIds);

        return messages.stream()
                .map(msg -> {
                    MessageResponse res = messageMapper.toMessageResponse(msg);
                    res.setAttachments(attachmentsByMessageId.getOrDefault(msg.getId(), List.of()));
                    res.setReactions(reactionsByMessageId.getOrDefault(msg.getId(), List.of()));
                    return res;
                })
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

        // Push a delivery event to every channel member except the sender.
        // Each recipient's app (even if not in this chat screen) will receive
        // this on /topic/users/{recipientId}/dm-delivery and send back an ack,
        // allowing the sender to see "✓✓ Đã nhận" as soon as the message lands
        // on the recipient's device — not only when they open the chat.
        DmDeliveryEvent deliveryEvent = DmDeliveryEvent.builder()
                .channelId(request.getChannelId())
                .senderId(authorId)
                .build();

        channelMemberRepository.findAllByChannelId(channelUuid).forEach(member -> {
            if (!member.getUserId().equals(UUID.fromString(authorId))) {
                messagingTemplate.convertAndSend(
                        "/topic/users/" + member.getUserId() + "/dm-delivery",
                        deliveryEvent
                );
            }
        });

        return response;
    }

    @Transactional
    public MessageResponse editMessage(String userId, String messageId, UpdateMessageRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Message message = getOwnedMessage(userId, messageId);
        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        message.setContent(request.getContent().trim());
        message.setEditedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        MessageResponse response = buildResponse(saved);
        messagingTemplate.convertAndSend("/topic/channels/" + saved.getChannelId(), response);
        return response;
    }

    @Transactional
    public MessageResponse unsendMessage(String userId, String messageId) {
        Message message = getOwnedMessage(userId, messageId);

        if (!Boolean.TRUE.equals(message.getIsDeleted())) {
            message.setIsDeleted(true);
            message.setContent("Tin nhắn đã được thu hồi");
            message.setEditedAt(LocalDateTime.now());
        }

        Message saved = messageRepository.save(message);
        MessageResponse response = buildResponse(saved);
        messagingTemplate.convertAndSend("/topic/channels/" + saved.getChannelId(), response);
        return response;
    }

    private Message getOwnedMessage(String userId, String messageId) {
        Message message = messageRepository.findById(UUID.fromString(messageId))
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        UUID uid = UUID.fromString(userId);
        if (!uid.equals(message.getAuthorId())) {
            throw new AppException(ErrorCode.MESSAGE_NOT_OWNER);
        }
        return message;
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

        MessageResponse res = messageMapper.toMessageResponse(message);
        res.setAttachments(attachments);
        res.setReactions(reactionService.getReactionsForMessage(message.getId()));
        return res;
    }
}