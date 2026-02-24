package com.hubble.entity;

import com.hubble.enums.PresenceStatus;
import com.hubble.enums.Theme;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

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
    @UuidGenerator
    String id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(unique = true)
    String phone;

    // Tag dùng để tìm kiếm / kết bạn, vd: john#1234
    @Column(unique = true, nullable = false)
    String tag;

    @Column(nullable = false)
    String password;

    @Column(nullable = false)
    String displayName;

    String avatarUrl;

    @Column(columnDefinition = "TEXT")
    String bio;

    // Trạng thái tùy chỉnh: "Đang học bài..."
    String customStatus;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    PresenceStatus presenceStatus = PresenceStatus.OFFLINE;

    LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    Theme theme = Theme.DARK;

    @Builder.Default
    String language = "vi";

    @Builder.Default
    boolean active = true;

    @Builder.Default
    boolean emailVerified = false;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
