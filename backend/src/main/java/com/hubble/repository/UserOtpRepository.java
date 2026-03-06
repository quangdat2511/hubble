package com.hubble.repository;

import com.hubble.entity.UserOtp;
import com.hubble.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, UUID> {

    Optional<UserOtp> findFirstByUserIdAndTypeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
            UUID userId, OtpType type, LocalDateTime now);

    @Modifying
    @Query("UPDATE UserOtp o SET o.isUsed = true WHERE o.userId = :userId AND o.type = :type AND o.isUsed = false")
    void invalidateAllByUserIdAndType(UUID userId, OtpType type);
}
