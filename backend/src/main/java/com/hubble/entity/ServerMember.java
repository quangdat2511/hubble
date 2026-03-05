package com.hubble.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
    name = "server_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "user_id"})
)
public class ServerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "server_id", nullable = false)
    UUID serverId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "nickname", length = 64)
    String nickname;

    @Column(name = "joined_at", updatable = false)
    LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
