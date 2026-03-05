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
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "owner_id", nullable = false)
    UUID ownerId;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    String iconUrl;

    @Column(name = "invite_code", unique = true, length = 16)
    String inviteCode;

    @Column(name = "is_public")
    @Builder.Default
    Boolean isPublic = false;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
