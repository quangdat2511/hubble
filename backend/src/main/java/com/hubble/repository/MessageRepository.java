package com.hubble.repository;

import com.hubble.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);
    Page<Message> findByChannelIdAndIsDeletedFalseAndContentIsNotNullOrderByCreatedAtDesc(UUID channelId, Pageable pageable);

    @Query(
            value = """
                    select *
                    from messages m
                    where m.channel_id = :channelId
                      and coalesce(m.is_deleted, false) = false
                      and m.content is not null
                      and m.content ~* '(https?://|www\\.)'
                    order by m.created_at desc
                    """,
            countQuery = """
                    select count(*)
                    from messages m
                    where m.channel_id = :channelId
                      and coalesce(m.is_deleted, false) = false
                      and m.content is not null
                      and m.content ~* '(https?://|www\\.)'
                    """,
            nativeQuery = true
    )
    Page<Message> findSharedLinkMessagesByChannelId(@Param("channelId") UUID channelId, Pageable pageable);

    void deleteAllByChannelId(UUID channelId);

    /** All non-deleted messages from others (never read / no last_read boundary). */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.channelId = :channelId
            AND m.authorId <> :userId
            AND (m.isDeleted IS NULL OR m.isDeleted = false)
            """)
    long countIncomingMessagesFromOthers(@Param("channelId") UUID channelId, @Param("userId") UUID userId);

    /** Non-deleted incoming messages strictly after last_read_at (readAt must be non-null). */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.channelId = :channelId
            AND m.authorId <> :userId
            AND (m.isDeleted IS NULL OR m.isDeleted = false)
            AND m.createdAt > :readAt
            """)
    long countIncomingMessagesAfterRead(
            @Param("channelId") UUID channelId,
            @Param("userId") UUID userId,
            @Param("readAt") LocalDateTime readAt
    );
}
