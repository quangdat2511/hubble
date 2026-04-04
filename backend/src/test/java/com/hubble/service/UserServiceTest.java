package com.hubble.service;

import com.hubble.dto.request.UpdateCustomStatusRequest;
import com.hubble.dto.request.UpdateProfileRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.enums.UserStatus;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();
        userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .build();
    }

    @Test
    void getUserById_UserExists_ReturnsUserResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.getUserById(userId));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void getUserByEmail_UserExists_ReturnsUserResponse() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getUserByEmail_UserNotFound_ThrowsException() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.getUserByEmail(email));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void findById_UserExists_ReturnsUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void findById_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.findById(userId));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void updateProfile_UserExistsAndPhoneNotDuplicated_UpdatesSuccessfully() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Display Name ");
        request.setPhone(" 0123456789 ");
        request.setBio("New Bio");
        request.setStatus(UserStatus.ONLINE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0123456789", userId)).thenReturn(false);

        UserResponse updatedResponse = UserResponse.builder().displayName("New Display Name").build();
        when(userMapper.toUserResponse(user)).thenReturn(updatedResponse);

        UserResponse result = userService.updateProfile(userId, request);

        assertEquals("New Display Name", user.getDisplayName());
        assertEquals("0123456789", user.getPhone());
        assertEquals("New Bio", user.getBio());
        assertEquals(UserStatus.ONLINE, user.getStatus());

        verify(userRepository).save(user);
        assertNotNull(result);
    }

    @Test
    void updateProfile_PhoneDuplicated_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("0123456789");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0123456789", userId)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userService.updateProfile(userId, request));
        assertEquals(ErrorCode.PHONE_EXISTED, exception.getErrorCode());
    }

    @Test
    void updateCustomStatus_UserExists_UpdatesSuccessfully() {
        UpdateCustomStatusRequest request = new UpdateCustomStatusRequest();
        request.setCustomStatus("Working remotely");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse updatedResponse = UserResponse.builder().customStatus("Working remotely").build();
        when(userMapper.toUserResponse(user)).thenReturn(updatedResponse);

        UserResponse result = userService.updateCustomStatus(userId, request);

        assertEquals("Working remotely", user.getCustomStatus());
        verify(userRepository).save(user);
        assertNotNull(result);
    }

    @Test
    void updateCustomStatus_UserNotFound_ThrowsException() {
        UpdateCustomStatusRequest request = new UpdateCustomStatusRequest();
        request.setCustomStatus("Working remotely");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.updateCustomStatus(userId, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }
}