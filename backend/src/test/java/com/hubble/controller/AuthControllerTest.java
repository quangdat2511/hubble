package com.hubble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.configuration.SecurityConfig;
import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.repository.UserRepository;
import com.hubble.repository.UserSessionRepository;
import com.hubble.security.JwtAuthenticationFilter;
import com.hubble.security.JwtService;
import com.hubble.security.UserPrincipal;
import com.hubble.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class AuthControllerTest {

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

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Test
    public void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("validuser")
                .email("test@example.com")
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
    public void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("us")
                .email("invalid-email")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void verifyEmailRegistration_ValidRequest_ReturnsToken() throws Exception {
        EmailVerifyOtpRequest request = EmailVerifyOtpRequest.builder()
                .email("test@example.com")
                .otpCode("123456")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.verifyEmailRegistration(any(EmailVerifyOtpRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));
    }

    @Test
    public void login_ValidRequest_ReturnsToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));
    }

    @Test
    public void login_InvalidRequest_ReturnsBadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void loginWithPhone_ValidRequest_ReturnsToken() throws Exception {
        PhoneLoginRequest request = PhoneLoginRequest.builder()
                .phone("0987654321")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .build();

        when(authService.loginWithPhone(any(PhoneLoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));
    }

    @Test
    public void loginWithGoogle_ValidRequest_ReturnsToken() throws Exception {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("google-id-token")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .build();

        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));
    }

    @Test
    public void sendPhoneOtp_ValidRequest_ReturnsOk() throws Exception {
        PhoneSendOtpRequest request = PhoneSendOtpRequest.builder()
                .phone("0987654321")
                .build();

        doNothing().when(authService).sendPhoneOtp(any(PhoneSendOtpRequest.class));

        mockMvc.perform(post("/api/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("OTP đã được gửi đến 0987654321"));
    }

    @Test
    public void verifyPhone_ValidRequest_ReturnsToken() throws Exception {
        PhoneVerifyOtpRequest request = PhoneVerifyOtpRequest.builder()
                .phone("0987654321")
                .otpCode("123456")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .build();

        when(authService.verifyPhoneAndLogin(any(PhoneVerifyOtpRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));
    }

    @Test
    public void refreshToken_ValidRequest_ReturnsToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"));
    }

    @Test
    public void logout_ValidRequest_ReturnsOk() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Đăng xuất thành công"));
    }

    @Test
    public void forgotPassword_ValidRequest_ReturnsOk() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Mã OTP đã được gửi đến email của bạn"));
    }

    @Test
    public void resetPassword_ValidRequest_ReturnsOk() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .otpCode("123456")
                .newPassword("newPassword123")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").value("Mật khẩu đã được đặt lại"));
    }

    @Test
    public void getCurrentUser_Authenticated_ReturnsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authService.getCurrentUser(userId.toString())).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.username").value("testuser"));
    }
}