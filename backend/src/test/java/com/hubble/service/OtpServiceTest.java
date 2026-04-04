package com.hubble.service;

import com.hubble.entity.UserOtp;
import com.hubble.enums.OtpType;
import com.hubble.repository.UserOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private UserOtpRepository userOtpRepository;

    @InjectMocks
    private OtpService otpService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
    }

    @Test
    void generateOtp_GeneratesAndSavesOtp() {
        String generatedOtp = otpService.generateOtp(userId, OtpType.EMAIL_VERIFY);

        assertNotNull(generatedOtp);
        assertEquals(6, generatedOtp.length());
        assertTrue(generatedOtp.matches("\\d{6}"));

        verify(userOtpRepository).invalidateAllByUserIdAndType(userId, OtpType.EMAIL_VERIFY);

        ArgumentCaptor<UserOtp> otpCaptor = ArgumentCaptor.forClass(UserOtp.class);
        verify(userOtpRepository).save(otpCaptor.capture());

        UserOtp savedOtp = otpCaptor.getValue();
        assertEquals(userId, savedOtp.getUserId());
        assertEquals(generatedOtp, savedOtp.getOtpCode());
        assertEquals(OtpType.EMAIL_VERIFY, savedOtp.getType());
        assertFalse(savedOtp.getIsUsed());
        assertTrue(savedOtp.getExpiredAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void verifyOtp_ValidOtp_ReturnsTrue() {
        String code = "123456";
        UserOtp otp = UserOtp.builder()
                .userId(userId)
                .otpCode(code)
                .type(OtpType.EMAIL_VERIFY)
                .isUsed(false)
                .build();

        when(userOtpRepository.findFirstByUserIdAndTypeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(OtpType.EMAIL_VERIFY), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));

        boolean isValid = otpService.verifyOtp(userId, code, OtpType.EMAIL_VERIFY);

        assertTrue(isValid);
        assertTrue(otp.getIsUsed());
        verify(userOtpRepository).save(otp);
    }

    @Test
    void verifyOtp_OtpNotFoundOrExpired_ReturnsFalse() {
        when(userOtpRepository.findFirstByUserIdAndTypeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(OtpType.EMAIL_VERIFY), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        boolean isValid = otpService.verifyOtp(userId, "123456", OtpType.EMAIL_VERIFY);

        assertFalse(isValid);
    }

    @Test
    void verifyOtp_WrongCode_ReturnsFalse() {
        UserOtp otp = UserOtp.builder()
                .userId(userId)
                .otpCode("654321")
                .type(OtpType.EMAIL_VERIFY)
                .isUsed(false)
                .build();

        when(userOtpRepository.findFirstByUserIdAndTypeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(OtpType.EMAIL_VERIFY), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));

        boolean isValid = otpService.verifyOtp(userId, "123456", OtpType.EMAIL_VERIFY);

        assertFalse(isValid);
        assertFalse(otp.getIsUsed());
    }
}