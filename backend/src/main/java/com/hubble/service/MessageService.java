package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.AttachmentResponse;
import com.hubble.dto.response.DmDeliveryEvent;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.response.ReactionResponse;
import com.hubble.dto.response.SharedContentItemResponse;
import com.hubble.dto.response.SharedContentPageResponse;
import com.hubble.dto.response.SmartReplyResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Channel;
import com.hubble.entity.Message;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.enums.SharedContentType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.MemberRoleRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    static final Pattern LINK_PATTERN = Pattern.compile("((?:https?://|www\\.)[^\\s<>()]+)", Pattern.CASE_INSENSITIVE);

    MessageRepository messageRepository;
    AttachmentRepository attachmentRepository;
    MessageMapper messageMapper;
    ChannelRepository channelRepository;
    ChannelMemberRepository channelMemberRepository;
    ChannelRoleRepository channelRoleRepository;
    ServerMemberRepository serverMemberRepository;
    MemberRoleRepository memberRoleRepository;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;
    SmartReplyService smartReplyService;
    ReactionService reactionService;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String userId, String channelId, int page, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        checkChannelAccess(channelUuid, userUuid);

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

        Map<UUID, User> authors = loadAuthors(messages.stream().map(Message::getAuthorId).distinct().toList());

        return messages.stream()
                .map(msg -> {
                    MessageResponse res = messageMapper.toMessageResponse(msg);
                    res.setAttachments(attachmentsByMessageId.getOrDefault(msg.getId(), List.of()));
                    res.setReactions(reactionsByMessageId.getOrDefault(msg.getId(), List.of()));
                    applyAuthorFields(res, authors.get(msg.getAuthorId()));
                    return res;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedContentPageResponse getSharedContent(
            String userId,
            String channelId,
            SharedContentType type,
            int page,
            int size
    ) {
        UUID channelUuid = requireDirectConversationChannel(UUID.fromString(userId), channelId);
        PageRequest pageRequest = PageRequest.of(page, size);

        return switch (type) {
            case MEDIA -> buildAttachmentPage(
                    type,
                    page,
                    size,
                    attachmentRepository.findSharedMediaByChannelId(channelUuid, pageRequest)
            );
            case FILE -> buildAttachmentPage(
                    type,
                    page,
                    size,
                    attachmentRepository.findSharedFilesByChannelId(channelUuid, pageRequest)
            );
            case LINK -> buildLinkPage(
                    page,
                    size,
                    messageRepository.findSharedLinkMessagesByChannelId(channelUuid, pageRequest)
            );
        };
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesBefore(String channelId, String userId, String beforeId, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        UUID beforeUuid = UUID.fromString(beforeId);
        checkChannelAccess(channelUuid, userUuid);

        List<Message> messages = messageRepository.findMessagesBefore(channelUuid, beforeUuid, size);

        if (messages.isEmpty()) return List.of();

        List<UUID> messageIds = messages.stream().map(Message::getId).toList();

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
        Map<UUID, User> authors = loadAuthors(messages.stream().map(Message::getAuthorId).distinct().toList());

        return messages.stream()
                .map(msg -> {
                    MessageResponse res = messageMapper.toMessageResponse(msg);
                    res.setAttachments(attachmentsByMessageId.getOrDefault(msg.getId(), List.of()));
                    res.setReactions(reactionsByMessageId.getOrDefault(msg.getId(), List.of()));
                    applyAuthorFields(res, authors.get(msg.getAuthorId()));
                    return res;
                })
                .toList();
    }

    private Map<UUID, User> loadAuthors(List<UUID> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> map = new HashMap<>();
        for (User u : userRepository.findAllById(authorIds)) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private void applyAuthorFields(MessageResponse res, User author) {
        if (res == null || author == null) {
            return;
        }
        res.setAuthorUsername(author.getUsername());
        res.setAuthorDisplayName(author.getDisplayName());
    }

    @Transactional
    public MessageResponse sendMessage(String authorId, CreateMessageRequest request) {
        UUID channelUuid = UUID.fromString(request.getChannelId());
        UUID authorUuid = UUID.fromString(authorId);
        checkChannelAccess(channelUuid, authorUuid);

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

        if ("TEXT".equalsIgnoreCase(request.getType())) {
            CompletableFuture.runAsync(() -> {
                SmartReplyResponse aiResponse = smartReplyService.generateSuggestions(saved.getContent());

                if (aiResponse != null && aiResponse.getSuggestions() != null && !aiResponse.getSuggestions().isEmpty()) {
                    aiResponse.setMessageAuthorId(authorId);

                    messagingTemplate.convertAndSend(
                            "/topic/channels/" + request.getChannelId() + "/suggestions",
                            aiResponse
                    );
                }
            }).exceptionally(ex -> {
                System.out.println("Error generating smart reply suggestions: " + ex.getMessage());
                return null;
            });
        }
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

    private UUID requireAccessibleChannel(UUID userId, String channelId) {
        UUID channelUuid = UUID.fromString(channelId);
        if (!channelRepository.existsById(channelUuid)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelUuid, userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return channelUuid;
    }

    private UUID requireDirectConversationChannel(UUID userId, String channelId) {
        UUID channelUuid = requireAccessibleChannel(userId, channelId);
        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        if (channel.getType() != ChannelType.DM && channel.getType() != ChannelType.GROUP_DM) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return channelUuid;
    }

    @Transactional
    public void deleteMessagesByChannel(UUID channelId) {
        messageRepository.deleteAllByChannelId(channelId);
    }

    private SharedContentPageResponse buildAttachmentPage(
            SharedContentType type,
            int page,
            int size,
            Page<Attachment> attachmentsPage
    ) {
        List<SharedContentItemResponse> items = attachmentsPage.getContent().stream()
                .map(attachment -> SharedContentItemResponse.builder()
                        .id(toString(attachment.getId()))
                        .messageId(toString(attachment.getMessageId()))
                        .type(type.name())
                        .url(attachment.getUrl())
                        .previewUrl(attachment.getUrl())
                        .filename(attachment.getFilename())
                        .contentType(attachment.getContentType())
                        .sizeBytes(attachment.getSizeBytes())
                        .createdAt(toString(attachment.getCreatedAt()))
                        .build())
                .toList();

        return SharedContentPageResponse.builder()
                .type(type.name())
                .page(page)
                .size(size)
                .hasMore(attachmentsPage.hasNext())
                .items(items)
                .build();
    }

    private SharedContentPageResponse buildLinkPage(int page, int size, Page<Message> messagePage) {
        List<SharedContentItemResponse> items = new ArrayList<>();

        for (Message message : messagePage.getContent()) {
            List<String> links = extractLinks(message.getContent());
            for (int i = 0; i < links.size(); i++) {
                String link = links.get(i);
                items.add(SharedContentItemResponse.builder()
                        .id(message.getId() + "#link#" + i)
                        .messageId(toString(message.getId()))
                        .type(SharedContentType.LINK.name())
                        .url(link)
                        .previewUrl(link)
                        .contentType("text/uri-list")
                        .messageContent(message.getContent())
                        .createdAt(toString(message.getCreatedAt()))
                        .build());
            }
        }

        return SharedContentPageResponse.builder()
                .type(SharedContentType.LINK.name())
                .page(page)
                .size(size)
                .hasMore(messagePage.hasNext())
                .items(items)
                .build();
    }

    static List<String> extractLinks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> links = new LinkedHashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            String match = trimTrailingPunctuation(matcher.group(1));
            if (!match.isBlank()) {
                links.add(match.startsWith("http://") || match.startsWith("https://") ? match : "https://" + match);
            }
        }
        return new ArrayList<>(links);
    }

    private static String trimTrailingPunctuation(String value) {
        if (value == null) {
            return "";
        }

        int end = value.length();
        while (end > 0) {
            char ch = value.charAt(end - 1);
            if (".,!?;:)]}".indexOf(ch) >= 0) {
                end--;
                continue;
            }
            break;
        }
        return value.substring(0, end).trim();
    }

    private MessageResponse buildResponse(Message message) {
        List<AttachmentResponse> attachments = attachmentRepository
                .findByMessageId(message.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .toList();

        MessageResponse res = messageMapper.toMessageResponse(message);
        res.setAttachments(attachments);
        res.setReactions(reactionService.getReactionsForMessage(message.getId()));
        userRepository.findById(message.getAuthorId()).ifPresent(u -> applyAuthorFields(res, u));
        return res;
    }

    private AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .messageId(attachment.getMessageId())
                .filename(attachment.getFilename())
                .url(attachment.getUrl())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }

    private void checkChannelAccess(UUID channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        // DMs and group DMs must always enforce channel membership, even though they do not belong to a server.
        if (channel.getType() == ChannelType.DM || channel.getType() == ChannelType.GROUP_DM) {
            if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return;
        }

        if (channel.getServerId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!serverMemberRepository.existsByServerIdAndUserId(channel.getServerId(), userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!Boolean.TRUE.equals(channel.getIsPrivate())) return;

        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) return;

        boolean hasRoleAccess = serverMemberRepository.findByServerIdAndUserId(channel.getServerId(), userId)
                .map(member -> {
                    List<UUID> roleIds = memberRoleRepository.findRoleIdsByMemberId(member.getId());
                    return !roleIds.isEmpty() && channelRoleRepository.existsByChannelIdAndRoleIdIn(channelId, roleIds);
                })
                .orElse(false);

        if (!hasRoleAccess) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesAround(String channelId, String userId, String messageId, int limit) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        UUID messageUuid = UUID.fromString(messageId);
        checkChannelAccess(channelUuid, userUuid);

        Message target = messageRepository.findById(messageUuid)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        if (Boolean.TRUE.equals(target.getIsDeleted())) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        int half = Math.max(1, limit / 2);
        List<Message> messages = messageRepository.findMessagesAroundId(channelUuid, messageUuid, half);
        messages = messages.stream()
                .sorted(java.util.Comparator.comparing(Message::getCreatedAt))
                .collect(Collectors.toList());

        if (messages.isEmpty()) return List.of();

        List<UUID> messageIds = messages.stream().map(Message::getId).toList();
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
        Map<UUID, User> authors = loadAuthors(messages.stream().map(Message::getAuthorId).distinct().toList());

        return messages.stream()
                .map(msg -> {
                    MessageResponse res = messageMapper.toMessageResponse(msg);
                    res.setAttachments(attachmentsByMessageId.getOrDefault(msg.getId(), List.of()));
                    res.setReactions(reactionsByMessageId.getOrDefault(msg.getId(), List.of()));
                    applyAuthorFields(res, authors.get(msg.getAuthorId()));
                    return res;
                })
                .toList();
    }
}
