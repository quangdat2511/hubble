package com.hubble.service;

import com.hubble.dto.response.SearchAttachmentResponse;
import com.hubble.dto.response.SearchChannelResponse;
import com.hubble.dto.response.SearchMemberResponse;
import com.hubble.dto.response.SearchMessageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Channel;
import com.hubble.entity.Message;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchService {

    MessageRepository messageRepository;
    AttachmentRepository attachmentRepository;
    ChannelMemberRepository channelMemberRepository;
    ChannelRepository channelRepository;
    FriendshipRepository friendshipRepository;
    UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Channel scope
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SearchMessageResponse> searchChannelMessages(
            String userId, String channelId, String q, int page, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        requireChannelMember(channelUuid, userUuid);
        validateQuery(q);

        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        Page<Message> messagePage = messageRepository.searchByChannelId(
                channelUuid, q, PageRequest.of(page, size));

        Map<UUID, User> authorMap = loadAuthorMapForMessages(messagePage.getContent());

        return messagePage.map(m -> toSearchMessageResponse(m, channel.getName(), authorMap));
    }

    @Transactional(readOnly = true)
    public List<SearchMemberResponse> searchChannelMembers(
            String userId, String channelId, String q) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        requireChannelMember(channelUuid, userUuid);

        List<User> users = channelMemberRepository
                .findUsersByChannelIdAndNameContaining(channelUuid, q);
        return users.stream().map(this::toSearchMemberResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchChannelMedia(String userId, String channelId) {
        UUID channelUuid = UUID.fromString(channelId);
        requireChannelMember(channelUuid, UUID.fromString(userId));
        List<Attachment> attachments = attachmentRepository.findMediaByChannelId(channelUuid);
        return toSearchAttachmentResponses(attachments, channelUuid);
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchChannelFiles(String userId, String channelId) {
        UUID channelUuid = UUID.fromString(channelId);
        requireChannelMember(channelUuid, UUID.fromString(userId));
        return toSearchAttachmentResponses(attachmentRepository.findFilesByChannelId(channelUuid), channelUuid);
    }

    @Transactional(readOnly = true)
    public List<SearchMessageResponse> searchChannelPins(String userId, String channelId, String q) {
        UUID channelUuid = UUID.fromString(channelId);
        requireChannelMember(channelUuid, UUID.fromString(userId));
        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        List<Message> pins = (q == null || q.isBlank())
                ? messageRepository.findPinnedByChannelId(channelUuid)
                : messageRepository.findPinnedByChannelIdContaining(channelUuid, q);

        Map<UUID, User> authorMap = loadAuthorMapForMessages(pins);
        return pins.stream().map(m -> toSearchMessageResponse(m, channel.getName(), authorMap)).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Server scope
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SearchMessageResponse> searchServerMessages(
            String userId, String serverId, String q, int page, int size) {
        UUID serverUuid = UUID.fromString(serverId);
        UUID userUuid = UUID.fromString(userId);
        validateQuery(q);

        List<UUID> channelIds = getMemberChannelIds(userUuid, serverUuid);
        if (channelIds.isEmpty()) return Page.empty();

        Map<UUID, String> channelNames = buildChannelNameMap(channelIds);
        Page<Message> messagePage = messageRepository.searchByChannelIds(
                channelIds, q, PageRequest.of(page, size));
        Map<UUID, User> authorMap = loadAuthorMapForMessages(messagePage.getContent());

        return messagePage.map(m -> toSearchMessageResponse(
                m, channelNames.getOrDefault(m.getChannelId(), ""), authorMap));
    }

    @Transactional(readOnly = true)
    public List<SearchMemberResponse> searchServerMembers(
            String userId, String serverId, String q) {
        UUID serverUuid = UUID.fromString(serverId);
        UUID userUuid = UUID.fromString(userId);
        List<UUID> channelIds = getMemberChannelIds(userUuid, serverUuid);
        if (channelIds.isEmpty()) return List.of();

        List<User> users = channelMemberRepository
                .findDistinctUsersByChannelIdsAndNameContaining(channelIds, q);
        return users.stream().map(this::toSearchMemberResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchChannelResponse> searchServerChannels(
            String userId, String serverId, String q) {
        UUID serverUuid = UUID.fromString(serverId);
        UUID userUuid = UUID.fromString(userId);
        List<UUID> memberChannelIds = getMemberChannelIds(userUuid, serverUuid);

        List<Channel> channels = channelRepository
                .findByServerIdAndNameContainingIgnoreCase(serverUuid, q == null ? "" : q);

        // Only return channels the user has access to (is a member of)
        return channels.stream()
                .filter(c -> memberChannelIds.contains(c.getId()))
                .map(this::toSearchChannelResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchServerMedia(String userId, String serverId) {
        List<UUID> channelIds = getMemberChannelIds(UUID.fromString(userId), UUID.fromString(serverId));
        if (channelIds.isEmpty()) return List.of();
        return attachmentRepository.findMediaByChannelIds(channelIds).stream()
                .map(a -> toSearchAttachmentResponseMultiChannel(a, channelIds))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchServerFiles(String userId, String serverId) {
        List<UUID> channelIds = getMemberChannelIds(UUID.fromString(userId), UUID.fromString(serverId));
        if (channelIds.isEmpty()) return List.of();
        return attachmentRepository.findFilesByChannelIds(channelIds).stream()
                .map(a -> toSearchAttachmentResponseMultiChannel(a, channelIds))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchMessageResponse> searchServerPins(String userId, String serverId, String q) {
        List<UUID> channelIds = getMemberChannelIds(UUID.fromString(userId), UUID.fromString(serverId));
        if (channelIds.isEmpty()) return List.of();

        Map<UUID, String> channelNames = buildChannelNameMap(channelIds);
        List<Message> pins = (q == null || q.isBlank())
                ? messageRepository.findPinnedByChannelIds(channelIds)
                : messageRepository.findPinnedByChannelIdsContaining(channelIds, q);

        Map<UUID, User> authorMap = loadAuthorMapForMessages(pins);
        return pins.stream().map(m -> toSearchMessageResponse(
                m, channelNames.getOrDefault(m.getChannelId(), ""), authorMap)).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DM scope
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SearchMemberResponse> searchDmFriends(String userId, String q) {
        UUID userUuid = UUID.fromString(userId);
        List<UUID> friendIds = friendshipRepository.findFriendIds(userUuid);
        if (friendIds.isEmpty()) return List.of();

        String queryLower = q == null ? "" : q.toLowerCase();
        return userRepository.findAllById(friendIds).stream()
                .filter(u -> queryLower.isBlank()
                        || (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(queryLower))
                        || u.getUsername().toLowerCase().contains(queryLower))
                .map(this::toSearchMemberResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchDmMedia(String userId) {
        List<UUID> channelIds = getDmChannelIds(UUID.fromString(userId));
        if (channelIds.isEmpty()) return List.of();
        return attachmentRepository.findMediaByChannelIds(channelIds).stream()
                .map(a -> toSearchAttachmentResponseMultiChannel(a, channelIds))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchDmFiles(String userId) {
        List<UUID> channelIds = getDmChannelIds(UUID.fromString(userId));
        if (channelIds.isEmpty()) return List.of();
        return attachmentRepository.findFilesByChannelIds(channelIds).stream()
                .map(a -> toSearchAttachmentResponseMultiChannel(a, channelIds))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void requireChannelMember(UUID channelId, UUID userId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateQuery(String q) {
        if (q == null || q.trim().length() < 2) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private List<UUID> getMemberChannelIds(UUID userId, UUID serverId) {
        return channelMemberRepository.findChannelIdsByUserIdAndServerId(userId, serverId);
    }

    private List<UUID> getDmChannelIds(UUID userId) {
        return channelRepository.findChannelIdsByUserIdAndTypeIn(
                userId, List.of(ChannelType.DM, ChannelType.GROUP_DM));
    }

    private Map<UUID, String> buildChannelNameMap(List<UUID> channelIds) {
        return channelRepository.findAllById(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, c -> c.getName() == null ? "" : c.getName()));
    }

    private Map<UUID, User> loadAuthorMapForMessages(List<Message> messages) {
        List<UUID> authorIds = messages.stream().map(Message::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) return Map.of();
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private SearchMessageResponse toSearchMessageResponse(Message m, String channelName, Map<UUID, User> authorMap) {
        User author = authorMap.get(m.getAuthorId());
        return SearchMessageResponse.builder()
                .id(m.getId().toString())
                .channelId(m.getChannelId().toString())
                .channelName(channelName)
                .authorId(m.getAuthorId().toString())
                .authorUsername(author != null ? author.getUsername() : null)
                .authorDisplayName(author != null ? author.getDisplayName() : null)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .content(m.getContent())
                .type(m.getType() != null ? m.getType().name() : null)
                .isPinned(m.getIsPinned())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .attachments(List.of())
                .build();
    }

    private SearchMemberResponse toSearchMemberResponse(User u) {
        return SearchMemberResponse.builder()
                .id(u.getId().toString())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .avatarUrl(u.getAvatarUrl())
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .build();
    }

    private SearchChannelResponse toSearchChannelResponse(Channel c) {
        return SearchChannelResponse.builder()
                .id(c.getId().toString())
                .name(c.getName())
                .type(c.getType() != null ? c.getType().name() : null)
                .topic(c.getTopic())
                .build();
    }

    /** Map an attachment to a response. Finds its channel by looking up the message. */
    private SearchAttachmentResponse toSearchAttachmentResponseMultiChannel(
            Attachment a, List<UUID> channelIds) {
        // We need channelId — look up the message to get it
        Message msg = messageRepository.findById(a.getMessageId()).orElse(null);
        String channelId = msg != null ? msg.getChannelId().toString() : null;
        return SearchAttachmentResponse.builder()
                .id(a.getId().toString())
                .messageId(a.getMessageId().toString())
                .channelId(channelId)
                .filename(a.getFilename())
                .url(a.getUrl())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build();
    }

    private List<SearchAttachmentResponse> toSearchAttachmentResponses(
            List<Attachment> attachments, UUID channelId) {
        return attachments.stream().map(a -> SearchAttachmentResponse.builder()
                .id(a.getId().toString())
                .messageId(a.getMessageId().toString())
                .channelId(channelId.toString())
                .filename(a.getFilename())
                .url(a.getUrl())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build()).toList();
    }
}
