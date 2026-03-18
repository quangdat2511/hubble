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
@Table(name = "channel_members")
@IdClass(ChannelMemberId.class)
public class ChannelMember {



    @Id
    @Column(name = "channel_id", nullable = false)
    UUID channelId;

    @Id
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", insertable = false, updatable = false)
    Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;



    @Column(name = "last_read_at")
    LocalDateTime lastReadAt;

    @Column(name = "is_muted")
    @Builder.Default
    Boolean isMuted = false;

    @Column(name = "is_pinned")
    @Builder.Default
    Boolean isPinned = false;

    @Column(name = "joined_at", updatable = false)
    LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
