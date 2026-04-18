package com.hubble.service;

import com.hubble.dto.response.FriendRequestResponse;
import com.hubble.dto.response.FriendUserResponse;
import com.hubble.entity.Friendship;
import com.hubble.entity.User;
import com.hubble.enums.FriendshipStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private FriendService friendService;

    private UUID currentUserId;
    private UUID targetUserId;
    private User targetUser;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        targetUser = User.builder()
                .id(targetUserId)
                .username("target_user")
                .displayName("Target User")
                .build();
    }

    @Test
    void searchUsers_EmptyQuery_ReturnsEmptyList() {
        List<FriendUserResponse> result = friendService.searchUsers(currentUserId, "   ");
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(anyString());
    }

    @Test
    void searchUsers_ValidQuery_ReturnsUsers() {
        User user1 = User.builder().id(UUID.randomUUID()).username("test1").build();
        User user2 = User.builder().id(currentUserId).username("test2").build();

        when(userRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc("test"))
                .thenReturn(List.of(user1, user2));
        when(friendshipRepository.findRelationBetween(currentUserId, user1.getId()))
                .thenReturn(Optional.empty());

        List<FriendUserResponse> result = friendService.searchUsers(currentUserId, "test#1234");

        assertEquals(1, result.size());
        assertEquals(user1.getId(), result.get(0).getId());
        assertEquals("NONE", result.get(0).getRelationStatus());
    }

    @Test
    void sendFriendRequest_SameUser_ThrowsException() {
        AppException exception = assertThrows(AppException.class,
                () -> friendService.sendFriendRequest(currentUserId, currentUserId));
        assertEquals(ErrorCode.CANNOT_FRIEND_SELF, exception.getErrorCode());
    }

    @Test
    void sendFriendRequest_TargetNotFound_ThrowsException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> friendService.sendFriendRequest(currentUserId, targetUserId));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void sendFriendRequest_AlreadyFriends_ThrowsException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        Friendship relation = Friendship.builder().status(FriendshipStatus.ACCEPTED).build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.sendFriendRequest(currentUserId, targetUserId));
        assertEquals(ErrorCode.ALREADY_FRIENDS, exception.getErrorCode());
    }

    @Test
    void sendFriendRequest_Blocked_ThrowsException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        Friendship relation = Friendship.builder().status(FriendshipStatus.BLOCKED).build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.sendFriendRequest(currentUserId, targetUserId));
        assertEquals(ErrorCode.FRIEND_REQUEST_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void sendFriendRequest_Success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.empty());

        Friendship savedFriendship = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(savedFriendship);

        FriendRequestResponse response = friendService.sendFriendRequest(currentUserId, targetUserId);

        assertNotNull(response);
        assertEquals(savedFriendship.getId(), response.getId());
        assertEquals("PENDING", response.getStatus());
        assertFalse(response.isIncoming());
    }

    @Test
    void sendFriendRequestByUsername_Success() {
        when(userRepository.findByUsername("target_user")).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.empty());

        Friendship savedFriendship = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(savedFriendship);

        FriendRequestResponse response = friendService.sendFriendRequestByUsername(currentUserId, "target_user");

        assertNotNull(response);
        assertEquals(savedFriendship.getId(), response.getId());
    }

    @Test
    void getIncomingRequests_Success() {
        Friendship request = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findByAddresseeIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING))
                .thenReturn(List.of(request));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        List<FriendRequestResponse> responses = friendService.getIncomingRequests(currentUserId);

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isIncoming());
        assertEquals("PENDING_INCOMING", responses.get(0).getUser().getRelationStatus());
    }

    @Test
    void getOutgoingRequests_Success() {
        Friendship request = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING))
                .thenReturn(List.of(request));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        List<FriendRequestResponse> responses = friendService.getOutgoingRequests(currentUserId);

        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isIncoming());
        assertEquals("PENDING_OUTGOING", responses.get(0).getUser().getRelationStatus());
    }

    @Test
    void acceptRequest_NotAddressee_ThrowsException() {
        UUID requestId = UUID.randomUUID();
        Friendship request = Friendship.builder()
                .id(requestId)
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(request));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.acceptRequest(currentUserId, requestId));
        assertEquals(ErrorCode.FRIEND_REQUEST_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void acceptRequest_Success() {
        UUID requestId = UUID.randomUUID();
        Friendship request = Friendship.builder()
                .id(requestId)
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(request));

        friendService.acceptRequest(currentUserId, requestId);

        assertEquals(FriendshipStatus.ACCEPTED, request.getStatus());
        verify(friendshipRepository).save(request);
    }

    @Test
    void declineRequest_Success() {
        UUID requestId = UUID.randomUUID();
        Friendship request = Friendship.builder()
                .id(requestId)
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(request));

        friendService.declineRequest(currentUserId, requestId);

        verify(friendshipRepository).delete(request);
    }

    @Test
    void getBlockedUsers_Success() {
        Friendship relation = Friendship.builder()
                .addresseeId(targetUserId)
                .status(FriendshipStatus.BLOCKED)
                .build();

        when(friendshipRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.BLOCKED))
                .thenReturn(List.of(relation));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        List<FriendUserResponse> responses = friendService.getBlockedUsers(currentUserId);

        assertEquals(1, responses.size());
        assertEquals("BLOCKED_BY_ME", responses.get(0).getRelationStatus());
    }

    @Test
    void blockUser_RelationExistsImRequester_UpdatesStatus() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        Friendship relation = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        friendService.blockUser(currentUserId, targetUserId);

        assertEquals(FriendshipStatus.BLOCKED, relation.getStatus());
        verify(friendshipRepository).save(relation);
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void blockUser_RelationExistsImAddressee_DeletesAndRecreates() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        Friendship relation = Friendship.builder()
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        friendService.blockUser(currentUserId, targetUserId);

        verify(friendshipRepository).delete(relation);
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(captor.capture());
        assertEquals(currentUserId, captor.getValue().getRequesterId());
        assertEquals(FriendshipStatus.BLOCKED, captor.getValue().getStatus());
    }

    @Test
    void unblockUser_NotRequester_ThrowsException() {
        Friendship relation = Friendship.builder()
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.BLOCKED)
                .build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.unblockUser(currentUserId, targetUserId));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void unblockUser_Success() {
        Friendship relation = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.BLOCKED)
                .build();
        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(relation));

        friendService.unblockUser(currentUserId, targetUserId);

        verify(friendshipRepository).delete(relation);
    }

    @Test
    void getFriends_Success() {
        Friendship relation = Friendship.builder()
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        when(friendshipRepository.findAcceptedByUserId(currentUserId, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(relation));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        List<FriendUserResponse> responses = friendService.getFriends(currentUserId);

        assertEquals(1, responses.size());
        assertEquals(targetUserId, responses.get(0).getId());
        assertEquals("FRIEND", responses.get(0).getRelationStatus());
    }

    @Test
    void declineRequest_NotRelatedUser_ThrowsException() {
        UUID requestId = UUID.randomUUID();
        Friendship request = Friendship.builder()
                .id(requestId)
                .requesterId(UUID.randomUUID())
                .addresseeId(UUID.randomUUID())
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(request));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.declineRequest(currentUserId, requestId));

        assertEquals(ErrorCode.FRIEND_REQUEST_NOT_FOUND, exception.getErrorCode());
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void acceptRequest_NotPendingStatus_ThrowsException() {
        UUID requestId = UUID.randomUUID();
        Friendship request = Friendship.builder()
                .id(requestId)
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(request));

        AppException exception = assertThrows(AppException.class,
                () -> friendService.acceptRequest(currentUserId, requestId));
        assertNotNull(exception.getErrorCode());
    }
}