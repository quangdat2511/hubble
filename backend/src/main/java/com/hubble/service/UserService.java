package com.hubble.service;

import com.hubble.dto.request.UpdateCustomStatusRequest;
import com.hubble.dto.request.UpdateProfileRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    private static final String AVATAR_FOLDER = "uploads/avatars/";

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }


    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDisplayName(request.getDisplayName());
        user.setPhone(request.getPhone());
        user.setBio(request.getBio());
        user.setStatus(request.getStatus());

        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateCustomStatus(UUID userId, UpdateCustomStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setCustomStatus(request.getCustomStatus());

        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateAvatar(UUID userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // create folder if not exists
        File folder = new File(AVATAR_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // safe filename
        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;
        String filePath = AVATAR_FOLDER + fileName;

        File dest = new File(filePath);
        file.transferTo(dest);

        // ⚠️ IMPORTANT: URL should NOT be local path
        user.setAvatarUrl("/uploads/avatars/" + fileName);

        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }
}