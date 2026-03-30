package com.hubble.service;

import com.hubble.dto.response.FriendUserResponse;
import com.hubble.dto.response.FriendRequestResponse;
import com.hubble.entity.Friendship;
import com.hubble.entity.User;
import com.hubble.enums.FriendshipStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendService {

    UserRepository userRepository;
    FriendshipRepository friendshipRepository;

    @Transactional(readOnly = true)
    public List<FriendUserResponse> searchUsers(UUID currentUserId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String normalized = normalizeSearchQuery(query);
        List<User> users = userRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(normalized);

        return users.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(user -> FriendUserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .status(user.getStatus() != null ? user.getStatus().name() : null)
                        .relationStatus(resolveRelationStatus(currentUserId, user.getId()))
                        .build())
                .toList();
    }

    @Transactional
    public FriendRequestResponse sendFriendRequest(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_SELF);
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Friendship relation = friendshipRepository.findRelationBetween(currentUserId, targetUserId).orElse(null);

        if (relation != null) {
            if (relation.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            }
            if (relation.getStatus() == FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
            }
            throw new AppException(ErrorCode.FRIEND_REQUEST_EXISTED);
        }

        Friendship created = friendshipRepository.save(
                Friendship.builder()
                        .requesterId(currentUserId)
                        .addresseeId(targetUserId)
                        .status(FriendshipStatus.PENDING)
                        .build()
        );

        return FriendRequestResponse.builder()
                .id(created.getId())
                .requesterId(created.getRequesterId())
                .addresseeId(created.getAddresseeId())
                .status(created.getStatus().name())
                .createdAt(created.getCreatedAt())
                .incoming(false)
                .user(FriendUserResponse.builder()
                        .id(target.getId())
                        .username(target.getUsername())
                        .displayName(target.getDisplayName())
                        .avatarUrl(target.getAvatarUrl())
                        .status(target.getStatus() != null ? target.getStatus().name() : null)
                        .relationStatus("PENDING_OUTGOING")
                        .build())
                .build();
    }

    @Transactional
    public FriendRequestResponse sendFriendRequestByUsername(UUID currentUserId, String username) {
        String normalizedUsername = normalizeSearchQuery(username);
        User target = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return sendFriendRequest(currentUserId, target.getId());
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getIncomingRequests(UUID currentUserId) {
        return friendshipRepository
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(request -> toFriendRequestResponse(request, currentUserId, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getOutgoingRequests(UUID currentUserId) {
        return friendshipRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(request -> toFriendRequestResponse(request, currentUserId, false))
                .toList();
    }

    @Transactional
    public void acceptRequest(UUID currentUserId, UUID requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getAddresseeId().equals(currentUserId)
                || friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void declineRequest(UUID currentUserId, UUID requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        boolean isAddressee = friendship.getAddresseeId().equals(currentUserId);
        boolean isRequester = friendship.getRequesterId().equals(currentUserId);

        if ((!isAddressee && !isRequester) || friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendUserResponse> getBlockedUsers(UUID currentUserId) {
        return friendshipRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.BLOCKED)
                .stream()
                .map(relation -> {
                    User user = userRepository.findById(relation.getAddresseeId())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                    return FriendUserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .status(user.getStatus() != null ? user.getStatus().name() : null)
                            .relationStatus("BLOCKED_BY_ME")
                            .build();
                })
                .toList();
    }

    @Transactional
    public void blockUser(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_SELF);
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Friendship relation = friendshipRepository.findRelationBetween(currentUserId, targetUserId).orElse(null);

        if (relation == null) {
            friendshipRepository.save(Friendship.builder()
                    .requesterId(currentUserId)
                    .addresseeId(targetUserId)
                    .status(FriendshipStatus.BLOCKED)
                    .build());
            return;
        }

        if (relation.getRequesterId().equals(currentUserId)) {
            relation.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(relation);
            return;
        }

        friendshipRepository.delete(relation);
        friendshipRepository.save(Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.BLOCKED)
                .build());
    }

    @Transactional
    public void unblockUser(UUID currentUserId, UUID targetUserId) {
        Friendship relation = friendshipRepository.findRelationBetween(currentUserId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (relation.getStatus() == FriendshipStatus.BLOCKED && relation.getRequesterId().equals(currentUserId)) {
            friendshipRepository.delete(relation);
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Transactional(readOnly = true)
    public List<FriendUserResponse> getFriends(UUID currentUserId) {
        return friendshipRepository.findAcceptedByUserId(currentUserId, FriendshipStatus.ACCEPTED)
                .stream()
                .map(relation -> {
                    UUID friendId = relation.getRequesterId().equals(currentUserId)
                            ? relation.getAddresseeId()
                            : relation.getRequesterId();
                    User user = userRepository.findById(friendId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                    return FriendUserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .status(user.getStatus() != null ? user.getStatus().name() : null)
                            .relationStatus("FRIEND")
                            .build();
                })
                .toList();
    }

    private FriendRequestResponse toFriendRequestResponse(Friendship request, UUID currentUserId, boolean incoming) {
        UUID otherUserId = incoming ? request.getRequesterId() : request.getAddresseeId();
        User user = userRepository.findById(otherUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return FriendRequestResponse.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .addresseeId(request.getAddresseeId())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .incoming(incoming)
                .user(FriendUserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .status(user.getStatus() != null ? user.getStatus().name() : null)
                        .relationStatus(incoming ? "PENDING_INCOMING" : "PENDING_OUTGOING")
                        .build())
                .build();
    }

    private String resolveRelationStatus(UUID currentUserId, UUID otherUserId) {
        Friendship relation = friendshipRepository.findRelationBetween(currentUserId, otherUserId).orElse(null);

        if (relation == null) {
            return "NONE";
        }

        if (relation.getStatus() == FriendshipStatus.ACCEPTED) {
            return "FRIEND";
        }

        if (relation.getStatus() == FriendshipStatus.PENDING) {
            return relation.getRequesterId().equals(currentUserId)
                    ? "PENDING_OUTGOING"
                    : "PENDING_INCOMING";
        }

        if (relation.getRequesterId().equals(currentUserId)) {
            return "BLOCKED_BY_ME";
        }

        return "BLOCKED_ME";
    }

    private String normalizeSearchQuery(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        int tagIndex = normalized.indexOf('#');
        if (tagIndex > 0) {
            return normalized.substring(0, tagIndex);
        }
        return normalized;
    }
}