package com.hubble.service;

import com.hubble.dto.response.UserStatusResponse;
import com.hubble.entity.User;
import com.hubble.enums.UserStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserStatusService {

    UserRepository userRepository;
    FriendshipRepository friendshipRepository;
    SimpMessagingTemplate messagingTemplate;

    // In-memory heartbeat tracker: userId -> last heartbeat time
    private static final Map<UUID, LocalDateTime> heartbeatMap = new ConcurrentHashMap<>();

    // Heartbeat timeout: if no heartbeat for 2 minutes, user is considered offline
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 120;

    /**
     * Called when user explicitly sets their status (Online, Idle, DND, Invisible/Offline).
     * Saves the choice to previousStatus so it can be restored after going offline.
     */
    @Transactional
    public UserStatusResponse updateStatus(UUID userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(newStatus);
        user.setPreviousStatus(newStatus);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        // Update heartbeat
        heartbeatMap.put(userId, LocalDateTime.now());

        UserStatusResponse response = buildStatusResponse(user);
        broadcastStatusToFriends(userId, response);
        return response;
    }

    /**
     * Called when user updates custom status text.
     */
    @Transactional
    public UserStatusResponse updateCustomStatus(UUID userId, String customStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String trimmed = customStatus != null ? customStatus.trim() : null;
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }
        user.setCustomStatus(trimmed);
        userRepository.save(user);

        UserStatusResponse response = buildStatusResponse(user);
        broadcastStatusToFriends(userId, response);
        return response;
    }

    /**
     * Heartbeat endpoint: keeps user online. Called periodically by the client.
     * If user was marked offline by stale check but is still alive, restore their previous status.
     */
    @Transactional
    public void heartbeat(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        heartbeatMap.put(userId, LocalDateTime.now());
        user.setLastSeenAt(LocalDateTime.now());

        // If user was knocked offline by stale check, restore their previous status
        if (user.getStatus() == UserStatus.OFFLINE
                && user.getPreviousStatus() != null
                && user.getPreviousStatus() != UserStatus.OFFLINE) {
            user.setStatus(user.getPreviousStatus());
            userRepository.save(user);
            broadcastStatusToFriends(userId, buildStatusResponse(user));
            return;
        }

        userRepository.save(user);
    }

    /**
     * Called when user explicitly goes offline (app closing / logout).
     * Saves current status to previousStatus for later restoration.
     */
    @Transactional
    public void goOffline(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Save current status so we can restore it when user comes back
        user.setPreviousStatus(user.getStatus());
        user.setStatus(UserStatus.OFFLINE);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        heartbeatMap.remove(userId);

        broadcastStatusToFriends(userId, buildStatusResponse(user));
    }

    /**
     * Called on user login / app start. Restores user's previous status.
     */
    @Transactional
    public void goOnline(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        heartbeatMap.put(userId, LocalDateTime.now());
        user.setLastSeenAt(LocalDateTime.now());

        // Restore previous status, default to ONLINE if none saved
        UserStatus restoredStatus = user.getPreviousStatus() != null
                ? user.getPreviousStatus()
                : UserStatus.ONLINE;

        UserStatus oldStatus = user.getStatus();
        user.setStatus(restoredStatus);
        userRepository.save(user);

        if (oldStatus != restoredStatus) {
            broadcastStatusToFriends(userId, buildStatusResponse(user));
        }
    }

    /**
     * Scheduled task: checks for stale users who haven't sent heartbeat and marks them offline.
     * Saves their current status to previousStatus before setting OFFLINE.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkStaleUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

        List<User> staleUsers = userRepository.findStaleOnlineUsers(threshold);

        for (User user : staleUsers) {
            // Double-check heartbeat map
            LocalDateTime lastHeartbeat = heartbeatMap.get(user.getId());
            if (lastHeartbeat != null && lastHeartbeat.isAfter(threshold)) {
                continue; // Still alive
            }

            log.info("User {} timed out, setting to OFFLINE", user.getId());
            user.setPreviousStatus(user.getStatus()); // Save for restoration
            user.setStatus(UserStatus.OFFLINE);
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.save(user);

            heartbeatMap.remove(user.getId());
            broadcastStatusToFriends(user.getId(), buildStatusResponse(user));
        }
    }

    /**
     * Build visible status response. If user is OFFLINE, hide custom status.
     */
    private UserStatusResponse buildStatusResponse(User user) {
        String visibleCustomStatus = user.getStatus() == UserStatus.OFFLINE
                ? null
                : user.getCustomStatus();

        return UserStatusResponse.builder()
                .userId(user.getId())
                .status(user.getStatus() != null ? user.getStatus().name() : "OFFLINE")
                .customStatus(visibleCustomStatus)
                .lastSeenAt(user.getLastSeenAt() != null ? user.getLastSeenAt().toString() : null)
                .build();
    }

    /**
     * Broadcast status change to all friends via STOMP.
     */
    private void broadcastStatusToFriends(UUID userId, UserStatusResponse statusResponse) {
        List<UUID> friendIds = friendshipRepository.findFriendIds(userId);

        for (UUID friendId : friendIds) {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + friendId + "/friend-status",
                    statusResponse
            );
        }
    }
}
