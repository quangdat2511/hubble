package com.hubble.repository;

import com.hubble.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenAndIsActiveTrue(String refreshToken);

    List<UserSession> findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(UUID userId);

    Optional<UserSession> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllByUserId(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.refreshToken = :refreshToken")
    void deactivateByRefreshToken(String refreshToken);
}