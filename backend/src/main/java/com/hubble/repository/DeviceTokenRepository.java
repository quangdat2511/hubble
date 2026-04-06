package com.hubble.repository;

import com.hubble.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findAllByUserId(UUID userId);
    boolean existsByToken(String token);
    void deleteByToken(String token);
}