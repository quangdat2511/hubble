package com.hubble.entity;

import com.hubble.enums.ServerInviteStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

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
        name = "server_invites",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_server_invites_pending",
                columnNames = {"server_id", "invitee_id", "status"}
        )
)
public class ServerInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "server_id", nullable = false)
    UUID serverId;

    @Column(name = "inviter_id", nullable = false)
    UUID inviterId;

    @Column(name = "invitee_id", nullable = false)
    UUID inviteeId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "server_invite_status")
    @Builder.Default
    ServerInviteStatus status = ServerInviteStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ServerInviteStatus.PENDING;
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plusDays(7);
        }
    }
}

