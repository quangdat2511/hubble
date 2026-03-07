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
    EmailService emailService;

    @Transactional
    public String register(RegisterRequest request) {
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

        String otp = otpService.generateOtp(savedUser.getId(), OtpType.EMAIL_VERIFY);
        emailService.sendOtpEmail(savedUser.getEmail(), otp, "Xác thực tài khoản Hubble");

        return "Vui lòng kiểm tra email để nhận mã OTP xác thực.";
    }

    @Transactional
    public TokenResponse verifyEmailRegistration(EmailVerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean valid = otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.EMAIL_VERIFY);
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        return createTokenResponse(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = otpService.generateOtp(user.getId(), OtpType.PASSWORD_RESET);
        emailService.sendOtpEmail(user.getEmail(), otp, "Khôi phục mật khẩu Hubble");
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.getEmailVerified()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return createTokenResponse(user);
    }

    public TokenResponse loginWithPhone(PhoneLoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return createTokenResponse(user);
    }

    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleUserInfo googleInfo = googleTokenVerifier.verify(request.getIdToken());

        if (googleInfo == null) {
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }

        User user = userRepository.findByEmail(googleInfo.getEmail()).orElse(null);

        if (user == null) {
            String baseUsername = googleInfo.getName() != null
                    ? googleInfo.getName().toLowerCase().replaceAll("[^a-z0-9_]", "_")
                    : "user_" + System.currentTimeMillis();

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
        }

        return createTokenResponse(user);
    }

    @Transactional
    public void sendPhoneOtp(PhoneSendOtpRequest request) {
        User user = userRepository.findByPhone(request.getPhone()).orElse(null);
        UUID userId;

        if (user != null) {
            userId = user.getId();
        } else {
            User tempUser = User.builder()
                    .username("phone_" + request.getPhone().replaceAll("[^0-9]", ""))
                    .phone(request.getPhone())
                    .authProvider(AuthProvider.PHONE)
                    .build();

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

    @Transactional
    public TokenResponse verifyPhoneAndLogin(PhoneVerifyOtpRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        boolean valid = otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PHONE_VERIFY);
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        user.setPhoneVerified(true);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            if (!user.getUsername().equals(request.getUsername().toLowerCase())
                    && userRepository.existsByUsername(request.getUsername().toLowerCase())) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
            user.setUsername(request.getUsername().toLowerCase());
            user.setDisplayName(request.getUsername());
        }

        userRepository.save(user);
        return createTokenResponse(user);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.validateToken(request.getRefreshToken())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        UserSession session = userSessionRepository
                .findByRefreshTokenAndIsActiveTrue(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        UUID userId = jwtService.getUserIdFromToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        session.setIsActive(false);
        userSessionRepository.save(session);

        return createTokenResponse(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.deactivateByRefreshToken(request.getRefreshToken());
    }

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

        userSessionRepository.deactivateAllByUserId(user.getId());
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

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