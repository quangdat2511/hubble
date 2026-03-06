package com.hubble.entity;

import com.hubble.enums.AuthProvider;
import com.hubble.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "auth_provider", nullable = false)
    AuthProvider authProvider;

    @Column(nullable = false, unique = true)
    String username;

    @Column(name = "display_name")
    String displayName;

    @Column(unique = true)
    String email;

    @Column(columnDefinition = "TEXT")
    String phone;

    @Column(name = "email_verified")
    Boolean emailVerified;

    @Column(name = "phone_verified")
    Boolean phoneVerified;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    String avatarUrl;

    @Column(columnDefinition = "TEXT")
    String bio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    UserStatus status;

    @Column(name = "custom_status")
    String customStatus;

    @Column(name = "last_seen_at")
    LocalDateTime lastSeenAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.OFFLINE;
        }
        if (this.authProvider == null) {
            this.authProvider = AuthProvider.LOCAL;
        }
        if (this.emailVerified == null) {
            this.emailVerified = false;
        }
        if (this.phoneVerified == null) {
            this.phoneVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}