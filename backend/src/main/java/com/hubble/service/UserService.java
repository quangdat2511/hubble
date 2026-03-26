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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    static final Path AVATAR_FOLDER = Paths.get(System.getProperty("user.dir"), "uploads", "avatars")
            .toAbsolutePath()
            .normalize();

    static final String AVATAR_URL_PREFIX = "/uploads/avatars/";
    static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
    static final String QR_SECRET = "zrsOcuJF0vzlwiCePOFSzQ3yijl2tbQIabcefdfsPHe6wZA9dfVZvxZA6UruGfMKYLr3RM9q";

    static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public UserResponse getUserById(UUID userId) {
        return userMapper.toUserResponse(findById(userId));
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
        User user = findById(userId);

        user.setDisplayName(request.getDisplayName());
        user.setPhone(request.getPhone());
        user.setBio(request.getBio());
        user.setStatus(request.getStatus());

        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    public UserResponse updateCustomStatus(UUID userId, UpdateCustomStatusRequest request) {
        User user = findById(userId);

        user.setCustomStatus(request.getCustomStatus());
        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateAvatar(UUID userId, MultipartFile file) throws IOException {
        validateAvatarFile(file);

        User user = findById(userId);
        Files.createDirectories(AVATAR_FOLDER);

        String fileName = UUID.randomUUID() + resolveExtension(file);
        Path newAvatarPath = AVATAR_FOLDER.resolve(fileName).normalize();
        if (!newAvatarPath.startsWith(AVATAR_FOLDER)) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }

        file.transferTo(newAvatarPath.toFile());

        String oldAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(AVATAR_URL_PREFIX + fileName);
        userRepository.save(user);

        deleteOldAvatar(oldAvatarUrl);
        return userMapper.toUserResponse(user);
    }

    public Path getAvatarPath(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        String avatarUrl = userRepository.findAvatarUrlById(userId)
                .filter(url -> !url.isBlank())
                .orElseThrow(() -> new AppException(ErrorCode.AVATAR_NOT_FOUND));

        String fileName = Paths.get(avatarUrl).getFileName().toString();
        Path avatarPath = AVATAR_FOLDER.resolve(fileName).normalize();

        if (!avatarPath.startsWith(AVATAR_FOLDER)) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }
        if (!Files.exists(avatarPath) || !Files.isRegularFile(avatarPath)) {
            throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
        }

        return avatarPath;
    }

    public AvatarResponse getAvatarResponse(UUID userId) throws IOException {
        User user = findById(userId);
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
        }

        Path avatarPath = getAvatarPath(userId);
        String fileName = avatarPath.getFileName().toString();
        String contentType = Files.probeContentType(avatarPath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return AvatarResponse.builder()
                .avatarUrl(avatarUrl)
                .fileName(fileName)
                .contentType(contentType)
                .build();
    }

    public String generateQrToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(QR_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public UUID parseQrToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(QR_SECRET.getBytes(StandardCharsets.UTF_8));

            String userId = Jwts.parser()
                    .verifyWith(key)
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
        return userMapper.toUserResponse(findById(parseQrToken(token)));
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return ".jpg";
        }

        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        try {
            String fileName = Paths.get(avatarUrl).getFileName().toString();
            Path oldAvatarPath = AVATAR_FOLDER.resolve(fileName).normalize();

            if (oldAvatarPath.startsWith(AVATAR_FOLDER)) {
                Files.deleteIfExists(oldAvatarPath);
            }
        } catch (Exception ignored) {
            System.err.println("Warning: could not delete old avatar: " + avatarUrl);
        }
    }
}
