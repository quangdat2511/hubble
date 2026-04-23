package com.hubble.service;

import com.hubble.dto.response.SearchAttachmentResponse;
import com.hubble.dto.response.SearchChannelResponse;
import com.hubble.dto.response.SearchMemberResponse;
import com.hubble.dto.response.SearchMessageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Channel;
import com.hubble.entity.Friendship;
import com.hubble.entity.Message;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.enums.FriendshipStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.MemberRoleRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchService {

    MessageRepository messageRepository;
    AttachmentRepository attachmentRepository;
    ChannelMemberRepository channelMemberRepository;
    ChannelRepository channelRepository;
    FriendshipRepository friendshipRepository;
    UserRepository userRepository;
    ServerMemberRepository serverMemberRepository;
    MemberRoleRepository memberRoleRepository;
    ChannelRoleRepository channelRoleRepository;

    @NonFinal
    @Value("${app.search.hybrid.enabled:true}")
    boolean hybridSearchEnabled;

    @NonFinal
    @Value("${app.search.hybrid.fts-candidate-cap:80}")
    int hybridFtsCandidateCap;

    @NonFinal
    @Value("${app.search.hybrid.fuzzy-candidate-cap:30}")
    int hybridFuzzyCandidateCap;

    @NonFinal
    @Value("${app.search.hybrid.fuzzy-min-query-length:3}")
    int hybridFuzzyMinQueryLength;

    @NonFinal
    @Value("${app.search.hybrid.similarity-threshold:0.22}")
    double hybridSimilarityThreshold;

    // ─────────────────────────────────────────────────────────────────────────
    // Channel scope
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SearchMessageResponse> searchChannelMessages(
            String userId, String channelId, String q, int page, int size) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        canAccessChannel(channelUuid, userUuid);
        validateQuery(q);

        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        Instant startedAt = Instant.now();
        boolean fallbackUsed = hybridSearchEnabled
                && q.trim().length() >= hybridFuzzyMinQueryLength;
        Page<Message> messagePage = hybridSearchEnabled
                ? messageRepository.searchByChannelIdHybrid(
                channelUuid,
                q,
                hybridFtsCandidateCap,
                hybridFuzzyCandidateCap,
                hybridFuzzyMinQueryLength,
                hybridSimilarityThreshold,
                PageRequest.of(page, size))
                : messageRepository.searchByChannelIdLegacy(
                channelUuid, q, PageRequest.of(page, size));

        Map<UUID, User> authorMap = loadAuthorMapForMessages(messagePage.getContent());
        logSearchTelemetry("channel", q, fallbackUsed, messagePage.isEmpty(), startedAt);

        return messagePage.map(m -> toSearchMessageResponse(m, channel.getName(), authorMap));
    }

    @Transactional(readOnly = true)
    public List<SearchMemberResponse> searchChannelMembers(
            String userId, String channelId, String q) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        canAccessChannel(channelUuid, userUuid);

        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        String safeQ = q == null ? "" : q;
        List<User> users;
        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
            users = channelMemberRepository
                    .findUsersByChannelIdAndNameContaining(channelUuid, safeQ);
        } else {
            users = serverMemberRepository
                    .findUsersByServerIdAndNameContaining(channel.getServerId(), safeQ);
        }
        Set<UUID> friendIds = loadFriendIdSet(userUuid);
        Map<UUID, Friendship> relationMap = loadRelationMap(userUuid, users);
        return users.stream().map(u -> toSearchMemberResponse(u, userUuid, friendIds, relationMap.get(u.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchChannelResponse> searchChannelChannels(
            String userId, String channelId, String q) {
        UUID channelUuid = UUID.fromString(channelId);
        UUID userUuid = UUID.fromString(userId);
        canAccessChannel(channelUuid, userUuid);

        Channel sourceChannel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        UUID serverId = sourceChannel.getServerId();
        if (serverId == null) {
            return List.of();
        }

        List<UUID> memberChannelIds = getMemberChannelIds(userUuid, serverId);
        if (memberChannelIds.isEmpty()) {
            return List.of();
        }

        String safeQ = q == null ? "" : q;
        return channelRepository.findByServerIdAndNameContainingIgnoreCase(serverId, safeQ).stream()
                .filter(c -> memberChannelIds.contains(c.getId()))
                .map(this::toSearchChannelResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchChannelMedia(String userId, String channelId) {
        UUID channelUuid = UUID.fromString(channelId);
        canAccessChannel(channelUuid, UUID.fromString(userId));
        List<Attachment> attachments = attachmentRepository.findMediaByChannelId(channelUuid);
        return toSearchAttachmentResponses(attachments, channelUuid);
    }

    @Transactional(readOnly = true)
    public List<SearchAttachmentResponse> searchChannelFiles(String userId, String channelId) {
        UUID channelUuid = UUID.fromString(channelId);
        canAccessChannel(channelUuid, UUID.fromString(userId));
        return toSearchAttachmentResponses(attachmentRepository.findFilesByChannelId(channelUuid), channelUuid);
    }

    @Transactional(readOnly = true)
    public List<SearchMessageResponse> searchChannelPins(String userId, String channelId, String q) {
        UUID channelUuid = UUID.fromString(channelId);
        canAccessChannel(channelUuid, UUID.fromString(userId));
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
        Instant startedAt = Instant.now();
        boolean fallbackUsed = hybridSearchEnabled
                && q.trim().length() >= hybridFuzzyMinQueryLength;
        Page<Message> messagePage = hybridSearchEnabled
                ? messageRepository.searchByChannelIdsHybrid(
                channelIds,
                q,
                hybridFtsCandidateCap,
                hybridFuzzyCandidateCap,
                hybridFuzzyMinQueryLength,
                hybridSimilarityThreshold,
                PageRequest.of(page, size))
                : messageRepository.searchByChannelIdsLegacy(
                channelIds, q, PageRequest.of(page, size));
        Map<UUID, User> authorMap = loadAuthorMapForMessages(messagePage.getContent());
        logSearchTelemetry("server", q, fallbackUsed, messagePage.isEmpty(), startedAt);

        return messagePage.map(m -> toSearchMessageResponse(
                m, channelNames.getOrDefault(m.getChannelId(), ""), authorMap));
    }

    @Transactional(readOnly = true)
    public List<SearchMemberResponse> searchServerMembers(
            String userId, String serverId, String q) {
        UUID serverUuid = UUID.fromString(serverId);
        UUID userUuid = UUID.fromString(userId);
        if (!serverMemberRepository.existsByServerIdAndUserId(serverUuid, userUuid)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String safeQ = q == null ? "" : q;
        List<User> users = serverMemberRepository
                .findUsersByServerIdAndNameContaining(serverUuid, safeQ);
        Set<UUID> friendIds = loadFriendIdSet(userUuid);
        Map<UUID, Friendship> relationMap = loadRelationMap(userUuid, users);
        return users.stream().map(u -> toSearchMemberResponse(u, userUuid, friendIds, relationMap.get(u.getId()))).toList();
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
        Set<UUID> friendIdSet = Set.copyOf(friendIds);
        return userRepository.findAllById(friendIds).stream()
                .filter(u -> queryLower.isBlank()
                        || (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(queryLower))
                        || u.getUsername().toLowerCase().contains(queryLower))
                .map(u -> toSearchMemberResponse(u, userUuid, friendIdSet, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SearchMessageResponse> searchDmMessages(
            String userId, String q, int page, int size) {
        UUID userUuid = UUID.fromString(userId);
        validateQuery(q);

        List<UUID> channelIds = getDmChannelIds(userUuid);
        if (channelIds.isEmpty()) {
            return Page.empty();
        }

        Instant startedAt = Instant.now();
        boolean fallbackUsed = hybridSearchEnabled
                && q.trim().length() >= hybridFuzzyMinQueryLength;
        Page<Message> messagePage = hybridSearchEnabled
                ? messageRepository.searchByChannelIdsHybrid(
                channelIds,
                q,
                hybridFtsCandidateCap,
                hybridFuzzyCandidateCap,
                hybridFuzzyMinQueryLength,
                hybridSimilarityThreshold,
                PageRequest.of(page, size))
                : messageRepository.searchByChannelIdsLegacy(
                channelIds, q, PageRequest.of(page, size));
        Map<UUID, User> authorMap = loadAuthorMapForMessages(messagePage.getContent());
        logSearchTelemetry("dm", q, fallbackUsed, messagePage.isEmpty(), startedAt);

        return messagePage.map(m -> toSearchMessageResponse(m, "", authorMap));
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

    /**
     * Checks if {@code userId} is allowed to access {@code channelId}.<br>
     * <ul>
     *   <li>DM channel → must be an explicit channel member</li>
     *   <li>Non-private server channel → any server member can access</li>
     *   <li>Private server channel → must be an explicit channel member OR hold a role that has access</li>
     * </ul>
     */
    private void canAccessChannel(UUID channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        // DM / GROUP_DM — gate on explicit channel membership
        if (channel.getServerId() == null) {
            if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return;
        }

        // Must be in the server at all
        if (!serverMemberRepository.existsByServerIdAndUserId(channel.getServerId(), userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Non-private: any server member can access
        if (!Boolean.TRUE.equals(channel.getIsPrivate())) return;

        // Private: explicit channel member
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) return;

        // Private: role-based access
        boolean hasRole = serverMemberRepository
                .findByServerIdAndUserId(channel.getServerId(), userId)
                .map(m -> {
                    List<UUID> roles = memberRoleRepository.findRoleIdsByMemberId(m.getId());
                    return !roles.isEmpty()
                            && channelRoleRepository.existsByChannelIdAndRoleIdIn(channelId, roles);
                })
                .orElse(false);

        if (!hasRole) throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private void validateQuery(String q) {
        if (q == null || q.trim().length() < 2) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    /**
     * Returns all channel IDs the user can access within a server:
     * public channels + private channels with explicit membership.
     * Returns empty list if the user is not a server member.
     */
    private List<UUID> getMemberChannelIds(UUID userId, UUID serverId) {
        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, userId)) {
            return List.of();
        }
        return channelRepository.findAccessibleChannelIdsByServerId(serverId, userId);
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

    private SearchMemberResponse toSearchMemberResponse(
            User u, UUID currentUserId, Set<UUID> friendIds, Friendship relation
    ) {
        return SearchMemberResponse.builder()
                .id(u.getId().toString())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .avatarUrl(u.getAvatarUrl())
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .isSelf(u.getId().equals(currentUserId))
                .isFriend(friendIds.contains(u.getId()))
                .friendshipState(resolveFriendshipState(currentUserId, u.getId(), relation))
                .build();
    }

    private Set<UUID> loadFriendIdSet(UUID userUuid) {
        return Set.copyOf(friendshipRepository.findFriendIds(userUuid));
    }

    private Map<UUID, Friendship> loadRelationMap(UUID userUuid, List<User> users) {
        List<UUID> targetIds = users.stream()
                .map(User::getId)
                .filter(id -> !id.equals(userUuid))
                .toList();
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        return friendshipRepository.findRelationsWithTargets(userUuid, targetIds).stream()
                .collect(Collectors.toMap(
                        f -> f.getRequesterId().equals(userUuid) ? f.getAddresseeId() : f.getRequesterId(),
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    private String resolveFriendshipState(UUID currentUserId, UUID targetUserId, Friendship relation) {
        if (currentUserId.equals(targetUserId)) {
            return "SELF";
        }
        if (relation == null) {
            return "NONE";
        }
        if (relation.getStatus() == FriendshipStatus.ACCEPTED) {
            return "FRIEND";
        }
        if (relation.getStatus() == FriendshipStatus.BLOCKED) {
            return "BLOCKED";
        }
        if (relation.getStatus() == FriendshipStatus.PENDING) {
            return relation.getRequesterId().equals(currentUserId)
                    ? "PENDING_OUTGOING"
                    : "PENDING_INCOMING";
        }
        return "NONE";
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

    private void logSearchTelemetry(
            String scope,
            String query,
            boolean fallbackUsed,
            boolean zeroResult,
            Instant startedAt
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        String latencyBucket = bucketizeLatency(latencyMs);
        log.info(
                "search_quality scope={} latency_ms={} latency_bucket={} fallback_used={} zero_result={} query_length={}",
                scope,
                latencyMs,
                latencyBucket,
                fallbackUsed,
                zeroResult,
                query == null ? 0 : query.trim().length()
        );
    }

    private String bucketizeLatency(long latencyMs) {
        if (latencyMs < 100) return "<100ms";
        if (latencyMs < 250) return "100-249ms";
        if (latencyMs < 500) return "250-499ms";
        if (latencyMs < 1000) return "500-999ms";
        return ">=1000ms";
    }
}


