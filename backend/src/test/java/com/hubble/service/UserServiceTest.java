package com.hubble.service;

import com.hubble.dto.response.AvatarResponse;
import com.hubble.entity.User;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getAvatarResponse_ExternalAvatarUrl_ReturnsRemoteAvatarWithoutFileLookup() throws IOException {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .avatarUrl("https://example.com/avatar/profile-photo.png")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AvatarResponse response = userService.getAvatarResponse(userId);

        assertEquals("https://example.com/avatar/profile-photo.png", response.getAvatarUrl());
        assertEquals("profile-photo.png", response.getFileName());
        assertEquals("image/*", response.getContentType());
    }
}
