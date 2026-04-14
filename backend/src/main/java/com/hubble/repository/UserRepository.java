package com.hubble.repository;

import com.hubble.entity.User;
import com.hubble.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, UUID id);

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);

    List<User> findAllByAvatarUrlStartingWith(String avatarUrlPrefix);
    List<User> findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

    @Modifying
    @Query("UPDATE User u SET u.status = :status, u.lastSeenAt = :lastSeenAt WHERE u.id = :userId")
    void updateStatusAndLastSeen(@Param("userId") UUID userId,
                                 @Param("status") UserStatus status,
                                 @Param("lastSeenAt") LocalDateTime lastSeenAt);

    @Query("SELECT u FROM User u WHERE u.status <> 'OFFLINE' AND u.lastSeenAt < :threshold")
    List<User> findStaleOnlineUsers(@Param("threshold") LocalDateTime threshold);
}
