package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserRepository;
import com.hubble.security.JwtService;
import com.hubble.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("giahuy@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn("Vui lòng kiểm tra email để nhận mã OTP xác thực.");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Vui lòng kiểm tra email để nhận mã OTP xác thực."));
    }

    @Test
    void verifyEmailRegistration_Success() throws Exception {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(authService.verifyEmailRegistration(any(EmailVerifyOtpRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void loginWithPhone_Success() throws Exception {
        PhoneLoginRequest request = PhoneLoginRequest.builder()
                .phone("0123456789")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(authService.loginWithPhone(any(PhoneLoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void loginWithGoogle_Success() throws Exception {
        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("google-token").build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void sendPhoneOtp_Success() throws Exception {
        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder().phone("0123456789").build();

        doNothing().when(authService).sendPhoneOtp(any(PhoneSendOtpRequest.class));

        mockMvc.perform(post("/api/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OTP đã được gửi đến 0123456789"));
    }

    @Test
    void verifyPhone_Success() throws Exception {
        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
                .phone("0123456789")
                .otpCode("123456")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(authService.verifyPhoneAndLogin(any(PhoneVerifyOtpRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void refreshToken_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("refresh-token").build();

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("new-token").build();
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("new-token"));
    }

    @Test
    void logout_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("refresh-token").build();

        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Đăng xuất thành công"));
    }

    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("giahuy@example.com").build();

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Mã OTP đã được gửi đến email của bạn"));
    }

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .newPassword("newpassword123")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Mật khẩu đã được đặt lại"));
    }

    @Test
    void getCurrentUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        UserResponse userResponse = UserResponse.builder().id(userId).username("giahuy").build();

        when(authService.getCurrentUser(userId.toString())).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value("giahuy"));
    }

    // ============ VALIDATION AND ERROR TEST CASES ============

    @Test
    void register_InvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("invalid-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_EmptyEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_WeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("giahuy@example.com")
                .password("123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AppException(ErrorCode.PASSWORD_INVALID));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_DuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("giahuy")
                .email("existing@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_EXISTED));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailRegistration_InvalidOtp() throws Exception {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("giahuy@example.com")
                .otpCode("000000")
                .build();

        when(authService.verifyEmailRegistration(any(EmailVerifyOtpRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_OTP));

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailRegistration_ExpiredOtp() throws Exception {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .build();

        when(authService.verifyEmailRegistration(any(EmailVerifyOtpRequest.class)))
                .thenThrow(new AppException(ErrorCode.OTP_EXPIRED));

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_InvalidEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_EmptyEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_EmptyPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("giahuy@example.com")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
//
//    @Test
//    void loginWithPhone_InvalidPhone() throws Exception {
//        PhoneLoginRequest request = PhoneLoginRequest.builder()
//                .phone("123")
//                .password("password123")
//                .build();
//
//        mockMvc.perform(post("/api/auth/login/phone")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().is4xxClientError());
//    }

    @Test
    void loginWithPhone_WrongPassword() throws Exception {
        PhoneLoginRequest request = PhoneLoginRequest.builder()
                .phone("0123456789")
                .password("wrongpassword")
                .build();

        when(authService.loginWithPhone(any(PhoneLoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithGoogle_InvalidToken() throws Exception {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("invalid-token")
                .build();

        when(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.GOOGLE_AUTH_FAILED));

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithGoogle_EmptyToken() throws Exception {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("")
                .build();

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

//    @Test
//    void sendPhoneOtp_InvalidPhone() throws Exception {
//        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder()
//                .phone("invalid")
//                .build();
//
//        mockMvc.perform(post("/api/auth/phone/send-otp")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().is4xxClientError());
//    }

    @Test
    void sendPhoneOtp_NonexistentPhone() throws Exception {
        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder()
                .phone("9999999999")
                .build();

        doThrow(new AppException(ErrorCode.USER_NOT_EXISTED))
                .when(authService).sendPhoneOtp(any(PhoneSendOtpRequest.class));

        mockMvc.perform(post("/api/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyPhone_InvalidOtp() throws Exception {
        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
                .phone("0123456789")
                .otpCode("000000")
                .build();

        when(authService.verifyPhoneAndLogin(any(PhoneVerifyOtpRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_OTP));

        mockMvc.perform(post("/api/auth/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_InvalidToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_TOKEN));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_ExpiredToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_InvalidToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        doThrow(new AppException(ErrorCode.INVALID_TOKEN))
                .when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_InvalidEmail() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        doThrow(new AppException(ErrorCode.USER_NOT_EXISTED))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void forgotPassword_EmptyEmail() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void resetPassword_InvalidOtp() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("000000")
                .newPassword("newpassword123")
                .build();

        doThrow(new AppException(ErrorCode.INVALID_OTP))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_WeakPassword() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .newPassword("weak")
                .build();

        doThrow(new AppException(ErrorCode.PASSWORD_INVALID))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_EmptyPassword() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("giahuy@example.com")
                .otpCode("123456")
                .newPassword("")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getCurrentUser_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);

        when(authService.getCurrentUser(userId.toString()))
                .thenThrow(new AppException(ErrorCode.USER_NOT_EXISTED));

        mockMvc.perform(get("/api/auth/me")
                        .principal(authentication))
                .andExpect(status().isNotFound());
    }
}