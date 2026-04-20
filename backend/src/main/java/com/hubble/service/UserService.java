package com.hubble.service;

import com.hubble.dto.request.UpdateCustomStatusRequest;
import com.hubble.dto.request.UpdateProfileRequest;
import com.hubble.dto.response.AvatarResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    AvatarStorageService avatarStorageService;

    private static final String QR_SECRET = "zrsOcuJF0vzlwiCePOFSzQ3yijl2tbQIabcefdfsPHe6wZA9dfVZvxZA6UruGfMKYLr3RM9q";

    public UserResponse getUserById(UUID userId) {
        User user = findById(userId);
        migrateLegacyAvatarIfNeeded(user);
        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        migrateLegacyAvatarIfNeeded(user);
        return userMapper.toUserResponse(user);
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public UserResponse updateAvatar(UUID userId, MultipartFile file) {
        User user = findById(userId);
        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = avatarStorageService.uploadAvatar(userId, file);

        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);
        deleteOldAvatar(oldAvatarUrl);

        return userMapper.toUserResponse(user);
    }

    public AvatarResponse getAvatarResponse(UUID userId) {
        User user = findById(userId);
        String avatarUrl = migrateLegacyAvatarIfNeeded(user);
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
        }

        String fileName = avatarStorageService.extractFileName(avatarUrl);
        return AvatarResponse.builder()
                .avatarUrl(avatarUrl)
                .fileName(fileName)
                .contentType(avatarStorageService.detectContentType(fileName))
                .build();
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);

        String displayName = normalize(request.getDisplayName());
        String phone = normalize(request.getPhone());
        String bio = normalize(request.getBio());

        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setBio(bio);
        user.setStatus(request.getStatus());
        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateCustomStatus(UUID userId, UpdateCustomStatusRequest request) {
        User user = findById(userId);
        user.setCustomStatus(normalize(request.getCustomStatus()));
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    private String migrateLegacyAvatarIfNeeded(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return avatarUrl;
        }

        if (!avatarStorageService.isLegacyAvatarUrl(avatarUrl)) {
            return avatarUrl;
        }

        String migratedUrl = avatarStorageService.migrateLegacyAvatar(
                user.getId(),
                avatarStorageService.resolveLegacyPath(avatarUrl)
        );
        user.setAvatarUrl(migratedUrl);
        userRepository.save(user);
        avatarStorageService.deleteLegacyAvatarQuietly(avatarUrl);
        return migratedUrl;
    }

    private void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        if (avatarStorageService.isLegacyAvatarUrl(avatarUrl)) {
            avatarStorageService.deleteLegacyAvatarQuietly(avatarUrl);
            return;
        }

        avatarStorageService.deleteAvatarByUrl(avatarUrl);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String generateQrToken(UUID userId) {

        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public UUID parseQrToken(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());

            String userId = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            return UUID.fromString(userId);
        } catch (IllegalArgumentException | JwtException e) {
            throw new AppException(ErrorCode.QR_TOKEN_INVALID);
        }
    }

    public UserResponse getUserFromQr(String token) {
        UUID userId = parseQrToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }
}
