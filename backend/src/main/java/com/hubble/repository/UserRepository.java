package com.hubble.repository;

import com.hubble.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);

    List<User> findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

    @Query("select u.avatarUrl from User u where u.id = :userId")
    Optional<String> findAvatarUrlById(@Param("userId") UUID userId);
}