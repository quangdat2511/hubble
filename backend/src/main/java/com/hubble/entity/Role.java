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
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "server_id", nullable = false)
    UUID serverId;

    @Column(nullable = false, length = 64)
    String name;

    @Column(nullable = false)
    @Builder.Default
    Integer color = 0;

    @Column(nullable = false)
    @Builder.Default
    Long permissions = 0L;

    @Column(nullable = false)
    @Builder.Default
    Short position = 0;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    Boolean isDefault = false;

    @Column(name = "display_separately", nullable = false)
    @Builder.Default
    Boolean displaySeparately = false;

    @Column(nullable = false)
    @Builder.Default
    Boolean mentionable = false;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
