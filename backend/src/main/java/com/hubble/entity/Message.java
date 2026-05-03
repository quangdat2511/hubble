package com.hubble.entity;

import com.hubble.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "channel_id", nullable = false)
    UUID channelId;

    @Column(name = "author_id", nullable = false)
    UUID authorId;

    @Column(name = "reply_to_id")
    UUID replyToId;

    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "message_type")
    @Builder.Default
    MessageType type = MessageType.TEXT;

    @Column(name = "is_pinned")
    @Builder.Default
    Boolean isPinned = false;

    @Column(name = "is_deleted")
    @Builder.Default
    Boolean isDeleted = false;

    @Column(name = "edited_at")
    LocalDateTime editedAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "mentioned_user_ids", columnDefinition = "uuid[]")
    List<UUID> mentionedUserIds;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
