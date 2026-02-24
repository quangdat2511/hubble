package com.hubble.repository;

import com.hubble.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    boolean existsByTag(String tag);

    Optional<User> findByEmail(String email);

    Optional<User> findByTag(String tag);

    // Tìm bạn qua email hoặc tag
    Optional<User> findByEmailOrTag(String email, String tag);
}
