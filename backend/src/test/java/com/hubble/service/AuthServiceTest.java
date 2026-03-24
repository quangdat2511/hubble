package com.hubble.service;

import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import com.hubble.entity.UserSession;
import com.hubble.enums.AuthProvider;
import com.hubble.enums.OtpType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.UserMapper;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.GoogleTokenVerifier;
import com.hubble.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("giahuy@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder().id(UUID.randomUUID()).email(request.getEmail()).build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(otpService.generateOtp(savedUser.getId(), OtpType.EMAIL_VERIFY)).thenReturn("123456");

        String result = authService.register(request);

        assertEquals("Vui lòng kiểm tra email để nhận mã OTP xác thực.", result);
        verify(emailService).sendOtpEmail(savedUser.getEmail(), "123456", "Xác thực tài khoản Hubble");
    }

    @Test
    void register_UserExisted_ThrowsAppException() {
        RegisterRequest request = RegisterRequest.builder().email("giahuy@example.com").build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    void verifyEmailRegistration_Success() {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .build();

        User user = User.builder().id(UUID.randomUUID()).email(request.getEmail()).emailVerified(false).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.EMAIL_VERIFY)).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(user.getId())).thenReturn("refresh_token");

        TokenResponse response = authService.verifyEmailRegistration(request);

        assertTrue(user.getEmailVerified());
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(userRepository).save(user);
    }

    @Test
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .passwordHash("encoded")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(user.getId())).thenReturn("refresh_token");

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
    }

    @Test
    void login_InvalidCredentials_ThrowsAppException() {
        LoginRequest request = LoginRequest.builder().email("giahuy@example.com").password("wrong").build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void loginWithGoogle_Success_NewUser() {
        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("valid_token").build();
        GoogleTokenVerifier.GoogleUserInfo googleInfo = new GoogleTokenVerifier.GoogleUserInfo(
                "giahuy@example.com", "Gia Huy", "url", true);

        when(googleTokenVerifier.verify(request.getIdToken())).thenReturn(googleInfo);
        when(userRepository.findByEmail(googleInfo.getEmail())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(googleInfo.getEmail())
                .authProvider(AuthProvider.GOOGLE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail())).thenReturn("access_token");

        TokenResponse response = authService.loginWithGoogle(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("old_refresh").build();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("giahuy@example.com").build();
        UserSession session = UserSession.builder().refreshToken("old_refresh").isActive(true).build();

        when(jwtService.validateToken(request.getRefreshToken())).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenAndIsActiveTrue(request.getRefreshToken())).thenReturn(Optional.of(session));
        when(jwtService.getUserIdFromToken(request.getRefreshToken())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(userId, user.getEmail())).thenReturn("new_access");

        TokenResponse response = authService.refreshToken(request);

        assertFalse(session.getIsActive());
        assertNotNull(response);
        assertEquals("new_access", response.getAccessToken());
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void logout_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("token").build();

        authService.logout(request);

        verify(userSessionRepository).deactivateByRefreshToken(request.getRefreshToken());
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .newPassword("newPass")
                .build();

        User user = User.builder().id(UUID.randomUUID()).email(request.getEmail()).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PASSWORD_RESET)).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedNew");

        authService.resetPassword(request);

        assertEquals("encodedNew", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(userSessionRepository).deactivateAllByUserId(user.getId());
    }

    @Test
    void getCurrentUser_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("giahuy").build();
        UserResponse userResponse = UserResponse.builder().id(userId).username("giahuy").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse response = authService.getCurrentUser(userId.toString());

        assertNotNull(response);
        assertEquals("giahuy", response.getUsername());
    }

    // ============ ADDITIONAL ERROR TEST CASES ============

    @Test
    void register_UsernameExisted_ThrowsAppException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("giahuy@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    void verifyEmailRegistration_InvalidOtp_ThrowsAppException() {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("giahuy@example.com")
                .otpCode("000000")
                .build();

        User user = User.builder().id(UUID.randomUUID()).email(request.getEmail()).emailVerified(false).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.EMAIL_VERIFY)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmailRegistration(request));
        assertEquals(ErrorCode.INVALID_OTP, exception.getErrorCode());
    }

    @Test
    void verifyEmailRegistration_UserNotFound_ThrowsAppException() {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("nonexistent@example.com")
                .otpCode("123456")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmailRegistration(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void login_UserNotFound_ThrowsAppException() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void login_WrongPassword_ThrowsAppException() {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("wrongpassword")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .passwordHash("encoded")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void login_EmailNotVerified_ThrowsAppException() {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .passwordHash("encoded")
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void loginWithGoogle_InvalidToken_ThrowsAppException() {
        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("invalid_token").build();

        when(googleTokenVerifier.verify(request.getIdToken())).thenThrow(new AppException(ErrorCode.GOOGLE_AUTH_FAILED));

        AppException exception = assertThrows(AppException.class, () -> authService.loginWithGoogle(request));
        assertEquals(ErrorCode.GOOGLE_AUTH_FAILED, exception.getErrorCode());
    }

    @Test
    void loginWithGoogle_ExistingUser_Success() {
        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("valid_token").build();
        GoogleTokenVerifier.GoogleUserInfo googleInfo = new GoogleTokenVerifier.GoogleUserInfo(
                "giahuy@example.com", "Gia Huy", "url", true);

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email(googleInfo.getEmail())
                .authProvider(AuthProvider.GOOGLE)
                .build();

        when(googleTokenVerifier.verify(request.getIdToken())).thenReturn(googleInfo);
        when(userRepository.findByEmail(googleInfo.getEmail())).thenReturn(Optional.of(existingUser));
        when(jwtService.generateAccessToken(existingUser.getId(), existingUser.getEmail())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(existingUser.getId())).thenReturn("refresh_token");

        TokenResponse response = authService.loginWithGoogle(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void refreshToken_InvalidToken_ThrowsAppException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("invalid_token").build();

        when(jwtService.validateToken(request.getRefreshToken())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void refreshToken_SessionNotFound_ThrowsAppException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("token").build();

        when(jwtService.validateToken(request.getRefreshToken())).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenAndIsActiveTrue(request.getRefreshToken())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void refreshToken_UserNotFound_ThrowsAppException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("token").build();
        UUID userId = UUID.randomUUID();
        UserSession session = UserSession.builder().refreshToken("token").isActive(true).build();

        when(jwtService.validateToken(request.getRefreshToken())).thenReturn(true);
        when(userSessionRepository.findByRefreshTokenAndIsActiveTrue(request.getRefreshToken())).thenReturn(Optional.of(session));
        when(jwtService.getUserIdFromToken(request.getRefreshToken())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void forgotPassword_UserNotFound_ThrowsAppException() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void forgotPassword_Success() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("giahuy@example.com")
                .build();

        User user = User.builder().id(UUID.randomUUID()).email(request.getEmail()).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(otpService.generateOtp(user.getId(), OtpType.PASSWORD_RESET)).thenReturn("123456");

        authService.forgotPassword(request);

        verify(emailService).sendOtpEmail(user.getEmail(), "123456", "Khôi phục mật khẩu Hubble");
    }

    @Test
    void resetPassword_InvalidOtp_ThrowsAppException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("000000")
                .newPassword("newPass123")
                .build();

        User user = User.builder().id(UUID.randomUUID()).email(request.getEmail()).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PASSWORD_RESET)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.INVALID_OTP, exception.getErrorCode());
    }

    @Test
    void resetPassword_UserNotFound_ThrowsAppException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("nonexistent@example.com")
                .otpCode("123456")
                .newPassword("newPass123")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void getCurrentUser_UserNotFound_ThrowsAppException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.getCurrentUser(userId.toString()));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

//    @Test
//    void sendPhoneOtp_Success() {
//        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder()
//                .phone("0123456789")
//                .build();
//
//        User user = User.builder().id(UUID.randomUUID()).phone(request.getPhone()).build();
//
//        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(user));
//        when(otpService.generateOtp(user.getId(), OtpType.PHONE_LOGIN)).thenReturn("123456");
//
//        authService.sendPhoneOtp(request);
//
//        verify(otpService).generateOtp(user.getId(), OtpType.PHONE_LOGIN);
//    }
//
//    @Test
//    void sendPhoneOtp_PhoneNotFound_ThrowsAppException() {
//        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder()
//                .phone("9999999999")
//                .build();
//
//        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.empty());
//
//        AppException exception = assertThrows(AppException.class, () -> authService.sendPhoneOtp(request));
//        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
//    }
//
//    @Test
//    void verifyPhoneAndLogin_Success() {
//        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
//                .phone("0123456789")
//                .otpCode("123456")
//                .build();
//
//        User user = User.builder().id(UUID.randomUUID()).phone(request.getPhone()).build();
//
//        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(user));
//        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PHONE_LOGIN)).thenReturn(true);
//        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("access_token");
//        when(jwtService.generateRefreshToken(user.getId())).thenReturn("refresh_token");
//
//        TokenResponse response = authService.verifyPhoneAndLogin(request);
//
//        assertNotNull(response);
//        assertEquals("access_token", response.getAccessToken());
//    }
//
//    @Test
//    void verifyPhoneAndLogin_InvalidOtp_ThrowsAppException() {
//        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
//                .phone("0123456789")
//                .otpCode("000000")
//                .build();
//
//        User user = User.builder().id(UUID.randomUUID()).phone(request.getPhone()).build();
//
//        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(user));
//        when(otpService.verifyOtp(user.getId(), request.getOtpCode(), OtpType.PHONE_LOGIN)).thenReturn(false);
//
//        AppException exception = assertThrows(AppException.class, () -> authService.verifyPhoneAndLogin(request));
//        assertEquals(ErrorCode.INVALID_OTP, exception.getErrorCode());
//    }

//    @Test
//    void verifyPhoneAndLogin_PhoneNotFound_ThrowsAppException() {
//        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
//                .phone("9999999999")
//                .otpCode("123456")
//                .build();
//
//        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.empty());
//
//        AppException exception = assertThrows(AppException.class, () -> authService.verifyPhoneAndLogin(request));
//        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
//    }
}