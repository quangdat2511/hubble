//package com.hubble.service;
//
//import com.hubble.dto.response.FriendRequestResponse;
//import com.hubble.dto.response.FriendUserResponse;
//import com.hubble.entity.Friendship;
//import com.hubble.entity.User;
//import com.hubble.enums.FriendshipStatus;
//import com.hubble.enums.UserStatus;
//import com.hubble.exception.AppException;
//import com.hubble.exception.ErrorCode;
//import com.hubble.repository.FriendshipRepository;
//import com.hubble.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class FriendServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private FriendshipRepository friendshipRepository;
//
//    @InjectMocks
//    private FriendService friendService;
//
//    private UUID currentUserId;
//    private UUID targetUserId;
//    private User targetUser;
//    private Friendship friendship;
//
//    @BeforeEach
//    void setUp() {
//        currentUserId = UUID.randomUUID();
//        targetUserId = UUID.randomUUID();
//
//        targetUser = User.builder()
//                .id(targetUserId)
//                .username("target_user")
//                .displayName("Target User")
//                .status(UserStatus.ONLINE)
//                .build();
//
//        friendship = Friendship.builder()
//                .id(UUID.randomUUID())
//                .requesterId(currentUserId)
//                .addresseeId(targetUserId)
//                .status(FriendshipStatus.PENDING)
//                .build();
//    }
//
//    @Test
//    void searchUsers_EmptyQuery_ReturnsEmptyList() {
//        List<FriendUserResponse> result = friendService.searchUsers(currentUserId, "   ");
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void searchUsers_ValidQuery_ReturnsUsers() {
//        when(userRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc("john"))
//                .thenReturn(List.of(targetUser));
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId))
//                .thenReturn(Optional.empty());
//
//        List<FriendUserResponse> result = friendService.searchUsers(currentUserId, "John#1234");
//
//        assertEquals(1, result.size());
//        assertEquals(targetUserId, result.get(0).getId());
//        assertEquals("NONE", result.get(0).getRelationStatus());
//    }
//
//    @Test
//    void sendFriendRequest_SelfRequest_ThrowsException() {
//        AppException exception = assertThrows(AppException.class,
//                () -> friendService.sendFriendRequest(currentUserId, currentUserId));
//        assertEquals(ErrorCode.CANNOT_FRIEND_SELF, exception.getErrorCode());
//    }
//
//    @Test
//    void sendFriendRequest_UserNotFound_ThrowsException() {
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());
//
//        AppException exception = assertThrows(AppException.class,
//                () -> friendService.sendFriendRequest(currentUserId, targetUserId));
//        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
//    }
//
//    @Test
//    void sendFriendRequest_Success() {
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.empty());
//        when(friendshipRepository.save(any(Friendship.class))).thenReturn(friendship);
//
//        FriendRequestResponse response = friendService.sendFriendRequest(currentUserId, targetUserId);
//
//        assertNotNull(response);
//        assertEquals(friendship.getId(), response.getId());
//        assertEquals("PENDING", response.getStatus());
//        verify(friendshipRepository).save(any(Friendship.class));
//    }
//
//    @Test
//    void sendFriendRequestByUsername_Success() {
//        when(userRepository.findByUsername("target_user")).thenReturn(Optional.of(targetUser));
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.empty());
//        when(friendshipRepository.save(any(Friendship.class))).thenReturn(friendship);
//
//        FriendRequestResponse response = friendService.sendFriendRequestByUsername(currentUserId, "target_user");
//
//        assertNotNull(response);
//        verify(userRepository).findByUsername("target_user");
//        verify(friendshipRepository).save(any(Friendship.class));
//    }
//
//    @Test
//    void acceptRequest_Success() {
//        friendship.setAddresseeId(currentUserId);
//        when(friendshipRepository.findById(friendship.getId())).thenReturn(Optional.of(friendship));
//
//        friendService.acceptRequest(currentUserId, friendship.getId());
//
//        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
//        verify(friendshipRepository).save(friendship);
//    }
//
//    @Test
//    void acceptRequest_WrongUser_ThrowsException() {
//        friendship.setRequesterId(currentUserId);
//        friendship.setAddresseeId(targetUserId);
//        when(friendshipRepository.findById(friendship.getId())).thenReturn(Optional.of(friendship));
//
//        AppException exception = assertThrows(AppException.class,
//                () -> friendService.acceptRequest(currentUserId, friendship.getId()));
//        assertEquals(ErrorCode.FRIEND_REQUEST_NOT_FOUND, exception.getErrorCode());
//    }
//
//    @Test
//    void declineRequest_Success() {
//        friendship.setAddresseeId(currentUserId);
//        when(friendshipRepository.findById(friendship.getId())).thenReturn(Optional.of(friendship));
//
//        friendService.declineRequest(currentUserId, friendship.getId());
//
//        verify(friendshipRepository).delete(friendship);
//    }
//
//    @Test
//    void blockUser_NewRelation_Success() {
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.empty());
//
//        friendService.blockUser(currentUserId, targetUserId);
//
//        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
//        verify(friendshipRepository).save(captor.capture());
//        assertEquals(FriendshipStatus.BLOCKED, captor.getValue().getStatus());
//        assertEquals(currentUserId, captor.getValue().getRequesterId());
//    }
//
//    @Test
//    void blockUser_ExistingRelation_UpdateSuccess() {
//        friendship.setStatus(FriendshipStatus.ACCEPTED);
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(friendship));
//
//        friendService.blockUser(currentUserId, targetUserId);
//
//        assertEquals(FriendshipStatus.BLOCKED, friendship.getStatus());
//        verify(friendshipRepository).save(friendship);
//    }
//
//    @Test
//    void unblockUser_Success() {
//        friendship.setStatus(FriendshipStatus.BLOCKED);
//        friendship.setRequesterId(currentUserId);
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(friendship));
//
//        friendService.unblockUser(currentUserId, targetUserId);
//
//        verify(friendshipRepository).delete(friendship);
//    }
//
//    @Test
//    void unblockUser_Unauthorized_ThrowsException() {
//        friendship.setStatus(FriendshipStatus.BLOCKED);
//        friendship.setRequesterId(targetUserId);
//        friendship.setAddresseeId(currentUserId);
//        when(friendshipRepository.findRelationBetween(currentUserId, targetUserId)).thenReturn(Optional.of(friendship));
//
//        AppException exception = assertThrows(AppException.class,
//                () -> friendService.unblockUser(currentUserId, targetUserId));
//        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
//    }
//
//    @Test
//    void getBlockedUsers_Success() {
//        friendship.setStatus(FriendshipStatus.BLOCKED);
//        when(friendshipRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.BLOCKED))
//                .thenReturn(List.of(friendship));
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//
//        List<FriendUserResponse> result = friendService.getBlockedUsers(currentUserId);
//
//        assertEquals(1, result.size());
//        assertEquals("BLOCKED_BY_ME", result.get(0).getRelationStatus());
//        assertEquals(targetUserId, result.get(0).getId());
//    }
//
//    @Test
//    void getFriends_Success() {
//        friendship.setStatus(FriendshipStatus.ACCEPTED);
//        when(friendshipRepository.findAcceptedByUserId(currentUserId, FriendshipStatus.ACCEPTED))
//                .thenReturn(List.of(friendship));
//        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
//
//        List<FriendUserResponse> result = friendService.getFriends(currentUserId);
//
//        assertEquals(1, result.size());
//        assertEquals("FRIEND", result.get(0).getRelationStatus());
//        assertEquals(targetUserId, result.get(0).getId());
//    }
//}