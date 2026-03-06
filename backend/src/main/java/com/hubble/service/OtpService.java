package com.hubble.service;

import com.hubble.entity.UserOtp;
import com.hubble.enums.OtpType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserOtpRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OtpService {

    final UserOtpRepository userOtpRepository;

    @Value("${otp.expiry-minutes:5}")
    int expiryMinutes;

    @Value("${otp.length:6}")
    int otpLength;

    /**
     * Generate and save a new OTP for a user.
     * Invalidates all previous unused OTPs of the same type.
     */
    @Transactional
    public String generateOtp(UUID userId, OtpType type) {
        // Invalidate old OTPs
        userOtpRepository.invalidateAllByUserIdAndType(userId, type);

        // Generate new OTP
        String otpCode = generateRandomOtp();

        UserOtp otp = UserOtp.builder()
                .userId(userId)
                .otpCode(otpCode)
                .type(type)
                .isUsed(false)
                .expiredAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();

        userOtpRepository.save(otp);

        // Log OTP to console for development (replace with SMS/Email service in production)
        log.info("═══════════════════════════════════════════");
        log.info("  OTP cho user {}: {} (type: {}, hết hạn sau {} phút)", userId, otpCode, type, expiryMinutes);
        log.info("═══════════════════════════════════════════");

        return otpCode;
    }

    /**
     * Verify an OTP code for a user.
     */
    @Transactional
    public boolean verifyOtp(UUID userId, String code, OtpType type) {
        var otpOpt = userOtpRepository
                .findFirstByUserIdAndTypeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
                        userId, type, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            return false;
        }

        UserOtp otp = otpOpt.get();
        if (!otp.getOtpCode().equals(code)) {
            return false;
        }

        // Mark as used
        otp.setIsUsed(true);
        userOtpRepository.save(otp);
        return true;
    }

    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
