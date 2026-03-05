package com.hubble.service;

import com.hubble.dto.request.UserCreationRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    @Transactional
    public UserResponse syncFirebaseUser(UserCreationRequest request) {
        if (userRepository.existsByFirebaseUid(request.getFirebaseUid())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByUsername(request.getUsername()) ||
        userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User newUser = userMapper.toUser(request);

        newUser.setStatus(com.hubble.enums.UserStatus.ONLINE);

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserResponse(savedUser);
    }
}