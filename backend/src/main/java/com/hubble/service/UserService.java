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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    private static final String QR_SECRET = "zrsOcuJF0vzlwiCePOFSzQ3yijl2tbQIabcefdfsPHe6wZA9dfVZvxZA6UruGfMKYLr3RM9q";

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
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setCustomStatus(request.getCustomStatus());

        userRepository.save(user);

        return userMapper.toUserResponse(user);
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

