package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.*;
import com.hubble.dto.response.TokenResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.service.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthController {

    AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<String>builder().result(message).build());
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyEmailRegistration(@Valid @RequestBody EmailVerifyOtpRequest request) {
        TokenResponse tokenResponse = authService.verifyEmailRegistration(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/email/send-otp")
    public ResponseEntity<ApiResponse<String>> sendEmailVerificationOtp(@Valid @RequestBody SendEmailOtpRequest request) {
        log.info("Request to send email OTP to: {}", request.getEmail());
        authService.sendEmailVerificationOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result("OTP đã được gửi đến email của bạn")
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/login/phone")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithPhone(@Valid @RequestBody PhoneLoginRequest request) {
        TokenResponse tokenResponse = authService.loginWithPhone(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        TokenResponse tokenResponse = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/phone/send-otp")
    public ResponseEntity<ApiResponse<String>> sendPhoneOtp(@Valid @RequestBody PhoneSendOtpRequest request) {
        authService.sendPhoneOtp(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result("OTP đã được gửi đến " + request.getPhone())
                .build());
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyPhone(@Valid @RequestBody PhoneVerifyOtpRequest request) {
        TokenResponse tokenResponse = authService.verifyPhoneAndLogin(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder().result(tokenResponse).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đăng xuất thành công").build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result("Mã OTP đã được gửi đến email của bạn")
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Mật khẩu đã được đặt lại").build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse userResponse = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder().result(userResponse).build());
    }
}