package com.hubble.service;

import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.entity.UserSettings;
import com.hubble.entity.UserSession;
import com.hubble.enums.AuthProvider;
import com.hubble.enums.NotificationType;
import com.hubble.enums.OtpType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSettingsRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.GoogleTokenVerifier;
import com.hubble.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserSessionRepository userSessionRepository;
    @Mock UserSettingsRepository userSettingsRepository;
    @Mock UserMapper userMapper;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock GoogleTokenVerifier googleTokenVerifier;
    @Mock OtpService otpService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;

    @InjectMocks AuthService authService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded_password")
                .emailVerified(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    private void mockTokenGeneration() {
        UserSession mockSession = UserSession.builder().id(UUID.randomUUID()).build();

        when(jwtService.generateRefreshToken(any())).thenReturn("mock_refresh_token");
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(mockSession);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("mock_access_token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        when(userMapper.toUserResponse(any())).thenReturn(new UserResponse());
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("new@example.com")
                .passwordHash("encoded_password")
                .emailVerified(false)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(otpService.generateOtp(any(), eq(OtpType.EMAIL_VERIFY))).thenReturn("123456");

        String response = authService.register(request);

        assertEquals("Vui lòng kiểm tra email để nhận mã OTP xác thực.", response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendOtpEmail(eq("new@example.com"), eq("123456"), anyString());
    }

    @Test
    void register_ThrowsException_WhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder().email("test@example.com").build();
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        RegisterRequest request = RegisterRequest.builder().email("new@example.com").username("testuser").build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    void verifyEmailRegistration_Success() {
        EmailVerifyOtpRequest request = new EmailVerifyOtpRequest("test@example.com", "123456");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(otpService.verifyOtp(eq(userId), eq("123456"), eq(OtpType.EMAIL_VERIFY))).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        mockTokenGeneration();

        TokenResponse response = authService.verifyEmailRegistration(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertTrue(mockUser.getEmailVerified());
    }

    @Test
    void verifyEmailRegistration_ThrowsException_WhenInvalidOtp() {
        EmailVerifyOtpRequest request = new EmailVerifyOtpRequest("test@example.com", "wrong_otp");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(otpService.verifyOtp(eq(userId), eq("wrong_otp"), eq(OtpType.EMAIL_VERIFY))).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmailRegistration(request));
        assertEquals(ErrorCode.INVALID_OTP, exception.getErrorCode());
    }

    @Test
    void verifyEmailRegistration_ThrowsException_WhenUserNotFound() {
        EmailVerifyOtpRequest request = new EmailVerifyOtpRequest("notfound@example.com", "123456");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmailRegistration(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "correct_password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        mockTokenGeneration();

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
    }

    @Test
    void login_SendsEnglishNewDeviceAlert_WhenLocaleIsEnglish() {
        LoginRequest request = new LoginRequest("test@example.com", "correct_password");
        UserSession savedSession = UserSession.builder().id(UUID.randomUUID()).build();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("X-Device-Name", "Google sdk_gphone64_x86_64");
        servletRequest.addHeader("X-Forwarded-For", "113.172.63.162");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateRefreshToken(any())).thenReturn("mock_refresh_token");
        when(userSessionRepository.existsByUserId(userId)).thenReturn(true);
        when(userSessionRepository.existsByUserIdAndDeviceName(userId, "Google sdk_gphone64_x86_64")).thenReturn(false);
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(savedSession);
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(
                UserSettings.builder()
                        .userId(userId)
                        .locale("en")
                        .notificationEnabled(true)
                        .newDeviceLoginAlertsEnabled(true)
                        .build()
        ));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("mock_access_token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        when(userMapper.toUserResponse(any())).thenReturn(new UserResponse());

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        verify(notificationService).dispatchNotification(
                eq(userId),
                eq(NotificationType.SYSTEM_ALERT),
                eq(savedSession.getId().toString()),
                eq("New login detected on Google sdk_gphone64_x86_64 (113.172.63.162). If this wasn't you, change your password now."),
                eq(false),
                eq(true)
        );
    }

    @Test
    void login_ThrowsException_WhenUnverifiedEmail() {
        mockUser.setEmailVerified(false);
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    void login_ThrowsException_WhenWrongPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong_password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void login_ThrowsException_WhenUserNotFound() {
        LoginRequest request = new LoginRequest("notfound@example.com", "password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void forgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(otpService.generateOtp(any(), eq(OtpType.PASSWORD_RESET))).thenReturn("654321");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(emailService).sendOtpEmail(eq("test@example.com"), eq("654321"), anyString());
    }

    @Test
    void forgotPassword_ThrowsException_WhenUserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("notfound@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "654321", "new_password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(otpService.verifyOtp(eq(userId), eq("654321"), eq(OtpType.PASSWORD_RESET))).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_password");

        assertDoesNotThrow(() -> authService.resetPassword(request));

        verify(userRepository).save(mockUser);
        verify(userSessionRepository).deactivateAllByUserId(userId);
    }

    @Test
    void resetPassword_ThrowsException_WhenInvalidOtp() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "wrong_otp", "new_password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(otpService.verifyOtp(eq(userId), eq("wrong_otp"), eq(OtpType.PASSWORD_RESET))).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.INVALID_OTP, exception.getErrorCode());
    }

    @Test
    void resetPassword_ThrowsException_WhenUserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest("notfound@example.com", "123456", "new_password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");
        UserSession mockSession = UserSession.builder().id(UUID.randomUUID()).isActive(true).build();

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenAndIsActiveTrue(anyString())).thenReturn(Optional.of(mockSession));
        when(jwtService.getUserIdFromToken(anyString())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        mockTokenGeneration();

        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertFalse(mockSession.getIsActive());
        verify(userSessionRepository).save(mockSession);
    }

    @Test
    void refreshToken_ThrowsException_WhenInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid_token");
        when(jwtService.validateToken(anyString())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void refreshToken_ThrowsException_WhenSessionNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_token_no_session");
        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenAndIsActiveTrue(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void logout_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");
        assertDoesNotThrow(() -> authService.logout(request));
        verify(userSessionRepository).deactivateByRefreshToken("valid_refresh_token");
    }

    @Test
    void loginWithGoogle_Success_ExistingUser() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid_google_token");
        GoogleTokenVerifier.GoogleUserInfo googleInfo = new GoogleTokenVerifier.GoogleUserInfo(
                "test@example.com", "Google User", "http://avatar.url", true
        );

        when(googleTokenVerifier.verify(anyString())).thenReturn(googleInfo);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        mockTokenGeneration();

        TokenResponse response = authService.loginWithGoogle(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
    }

    @Test
    void loginWithGoogle_Success_NewUser() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid_google_token");
        GoogleTokenVerifier.GoogleUserInfo googleInfo = new GoogleTokenVerifier.GoogleUserInfo(
                "newgoogle@example.com", "Google User", "http://avatar.url", true
        );

        when(googleTokenVerifier.verify(anyString())).thenReturn(googleInfo);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        mockTokenGeneration();

        TokenResponse response = authService.loginWithGoogle(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithGoogle_ThrowsException_WhenAuthFailed() {
        GoogleLoginRequest request = new GoogleLoginRequest("invalid_google_token");
        when(googleTokenVerifier.verify(anyString())).thenReturn(null);

        AppException exception = assertThrows(AppException.class, () -> authService.loginWithGoogle(request));
        assertEquals(ErrorCode.GOOGLE_AUTH_FAILED, exception.getErrorCode());
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserResponse(mockUser)).thenReturn(new UserResponse());

        UserResponse response = authService.getCurrentUser(userId.toString());

        assertNotNull(response);
        verify(userRepository).findById(userId);
    }

    @Test
    void getCurrentUser_ThrowsException_WhenUserNotFound() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.getCurrentUser(randomId.toString()));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }
}
