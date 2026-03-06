package com.hubble.service;

import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.entity.UserSession;
import com.hubble.enums.AuthProvider;
import com.hubble.enums.DeviceType;
import com.hubble.enums.OtpType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.GoogleTokenVerifier;
import com.hubble.security.JwtService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {

    UserRepository userRepository;
    UserSessionRepository userSessionRepository;
    UserMapper userMapper;
    JwtService jwtService;
    PasswordEncoder passwordEncoder;
    GoogleTokenVerifier googleTokenVerifier;
    OtpService otpService;

    // ─── Email/Password Registration ─────────────────────────────

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Check existing
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = User.builder()
                .username(request.getUsername().toLowerCase())
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        return createTokenResponse(savedUser);
    }

    // ─── Email/Password Login ────────────────────────────────────

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User logged in: {}", user.getEmail());
        return createTokenResponse(user);
    }

    // ─── Phone/Password Login ────────────────────────────────────

    public TokenResponse loginWithPhone(PhoneLoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User logged in with phone: {}", user.getPhone());
        return createTokenResponse(user);
    }

    // ─── Google OAuth2 Login ─────────────────────────────────────

    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleUserInfo googleInfo = googleTokenVerifier.verify(request.getIdToken());
        if (googleInfo == null) {
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }

        // Find or create user
        User user = userRepository.findByEmail(googleInfo.getEmail()).orElse(null);

        if (user == null) {
            // Create new user from Google info
            String baseUsername = googleInfo.getName() != null
                    ? googleInfo.getName().toLowerCase().replaceAll("[^a-z0-9_]", "_")
                    : "user_" + System.currentTimeMillis();

            // Ensure unique username
            String username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + "_" + counter++;
            }

            user = User.builder()
                    .username(username)
                    .displayName(googleInfo.getName())
                    .email(googleInfo.getEmail())
                    .avatarUrl(googleInfo.getPictureUrl())
                    .authProvider(AuthProvider.GOOGLE)
                    .emailVerified(googleInfo.isEmailVerified())
                    .phoneVerified(false)
                    .build();

            user = userRepository.save(user);
            log.info("New Google user registered: {}", user.getEmail());
        } else {
            log.info("Existing user logged in via Google: {}", user.getEmail());
        }

        return createTokenResponse(user);
    }

    // ─── Phone OTP Send ──────────────────────────────────────────

    @Transactional
    public void sendPhoneOtp(PhoneSendOtpRequest request) {
        // Check if user exists with this phone — if not, we'll create on verify
        User user = userRepository.findByPhone(request.getPhone()).orElse(null);

        UUID userId;
        if (user != null) {
            userId = user.getId();
        } else {
            // Create a placeholder user for OTP storage
            // This will be finalized during verification
            User tempUser = User.builder()
                    .username("phone_" + request.getPhone().replaceAll("[^0-9]", ""))
                    .phone(request.getPhone())
                    .authProvider(AuthProvider.PHONE)
                    .build();

            // Check if temp username exists
            String username = tempUser.getUsername();
            int counter = 1;
            while (userRepository.existsByUsername(username)) {
                username = tempUser.getUsername() + "_" + counter++;
            }
            tempUser.setUsername(username);

            tempUser = userRepository.save(tempUser);
            userId = tempUser.getId();
        }

        otpService.generateOtp(userId, OtpType.PHONE_VERIFY);
    }

    // ─── Phone OTP Verify & Register/Login ───────────────────────

    @Transactional
    public TokenResponse verifyPhoneAndLogin(PhoneVerifyOtpRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        boolean valid = otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PHONE_VERIFY);
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Mark phone as verified
        user.setPhoneVerified(true);

        // If password provided (registration flow), set it
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // If username provided, update it
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            if (!user.getUsername().equals(request.getUsername().toLowerCase())
                    && userRepository.existsByUsername(request.getUsername().toLowerCase())) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
            user.setUsername(request.getUsername().toLowerCase());
            user.setDisplayName(request.getUsername());
        }

        userRepository.save(user);
        log.info("Phone verified and logged in: {}", user.getPhone());
        return createTokenResponse(user);
    }

    // ─── Token Refresh ───────────────────────────────────────────

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token in JWT
        if (!jwtService.validateToken(request.getRefreshToken())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // Find active session with this refresh token
        UserSession session = userSessionRepository
                .findByRefreshTokenAndIsActiveTrue(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        UUID userId = jwtService.getUserIdFromToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Invalidate old session
        session.setIsActive(false);
        userSessionRepository.save(session);

        // Create new tokens
        return createTokenResponse(user);
    }

    // ─── Logout ──────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.deactivateByRefreshToken(request.getRefreshToken());
        log.info("User logged out, refresh token invalidated");
    }

    // ─── Forgot Password ─────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = otpService.generateOtp(user.getId(), OtpType.PASSWORD_RESET);

        // TODO: Send OTP via email (for now, logged to console)
        log.info("Password reset OTP sent to {}: {}", user.getEmail(), otp);
    }

    // ─── Reset Password ──────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean valid = otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PASSWORD_RESET);
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions
        userSessionRepository.deactivateAllByUserId(user.getId());
        log.info("Password reset successful for: {}", user.getEmail());
    }

    // ─── Get Current User ────────────────────────────────────────

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    // ─── Private Helpers ─────────────────────────────────────────

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Save session
        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshToken(refreshToken)
                .deviceType(DeviceType.MOBILE)
                .isActive(true)
                .build();
        userSessionRepository.save(session);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
