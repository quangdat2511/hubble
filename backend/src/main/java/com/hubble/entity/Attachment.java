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
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "message_id", nullable = false)
    UUID messageId;

    @Column(name = "filename", columnDefinition = "TEXT", nullable = false)
    String filename;

    @Column(name = "url", columnDefinition = "TEXT", nullable = false)
    String url;

    @Column(name = "content_type", length = 128)
    String contentType;

    @Column(name = "size_bytes")
    Long sizeBytes;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
