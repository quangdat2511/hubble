package com.hubble.entity;

import com.hubble.enums.ChannelType;
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
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    /** Nullable – DM / GROUP_DM channels have no server */
    @Column(name = "server_id")
    UUID serverId;

    /** Nullable – top-level channels have no parent */
    @Column(name = "parent_id")
    UUID parentId;

    @Column(name = "name", length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false)
    ChannelType type;

    @Column(name = "topic", columnDefinition = "TEXT")
    String topic;

    @Column(name = "position")
    @Builder.Default
    Short position = 0;

    @Column(name = "is_private")
    @Builder.Default
    Boolean isPrivate = false;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
