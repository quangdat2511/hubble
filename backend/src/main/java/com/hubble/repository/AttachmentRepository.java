package com.hubble.repository;

import com.hubble.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByMessageId(UUID messageId);
    List<Attachment> findByMessageIdIn(List<UUID> messageIds);

    // ── Search: media (images) by channel ─────────────────────────────────────
    @Query(value = """
            SELECT a.* FROM attachments a
            JOIN messages m ON a.message_id = m.id
            WHERE m.channel_id = :channelId
              AND (m.is_deleted IS NULL OR m.is_deleted = false)
              AND a.content_type LIKE 'image/%'
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Attachment> findMediaByChannelId(@Param("channelId") UUID channelId);

    // ── Search: non-image files by channel ────────────────────────────────────
    @Query(value = """
            SELECT a.* FROM attachments a
            JOIN messages m ON a.message_id = m.id
            WHERE m.channel_id = :channelId
              AND (m.is_deleted IS NULL OR m.is_deleted = false)
              AND (a.content_type IS NULL OR a.content_type NOT LIKE 'image/%')
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Attachment> findFilesByChannelId(@Param("channelId") UUID channelId);

    // ── Search: media by multiple channels (server-scope / DM-scope) ──────────
    @Query(value = """
            SELECT a.* FROM attachments a
            JOIN messages m ON a.message_id = m.id
            WHERE m.channel_id IN :channelIds
              AND (m.is_deleted IS NULL OR m.is_deleted = false)
              AND a.content_type LIKE 'image/%'
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Attachment> findMediaByChannelIds(@Param("channelIds") List<UUID> channelIds);

    // ── Search: non-image files by multiple channels ───────────────────────────
    @Query(value = """
            SELECT a.* FROM attachments a
            JOIN messages m ON a.message_id = m.id
            WHERE m.channel_id IN :channelIds
              AND (m.is_deleted IS NULL OR m.is_deleted = false)
              AND (a.content_type IS NULL OR a.content_type NOT LIKE 'image/%')
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Attachment> findFilesByChannelIds(@Param("channelIds") List<UUID> channelIds);
}
