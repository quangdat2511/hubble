package com.hubble.repository;

import com.hubble.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);
    void deleteAllByChannelId(UUID channelId);

    // ── Search: channel-scope FTS ──────────────────────────────────────────────
    @Query(
        value = """
            SELECT * FROM messages
            WHERE channel_id = :channelId
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            ORDER BY created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM messages
            WHERE channel_id = :channelId
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            """,
        nativeQuery = true
    )
    Page<Message> searchByChannelIdLegacy(@Param("channelId") UUID channelId, @Param("query") String query, Pageable pageable);

    @Query(
        value = """
            WITH params AS (
                SELECT trim(:query) AS raw_q,
                       unaccent_immutable(lower(trim(:query))) AS norm_q,
                       plainto_tsquery('simple', trim(:query)) AS ts_q
            ),
            fts_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id = :channelId
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND to_tsvector('simple', coalesce(m.content, '')) @@ p.ts_q
                ORDER BY
                    ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :ftsCandidateCap
            ),
            fuzzy_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id = :channelId
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND length(p.norm_q) >= :fuzzyMinQueryLength
                  AND (
                        unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%')
                        OR similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) >= :similarityThreshold
                  )
                ORDER BY
                    CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 1
                        ELSE 0
                    END DESC,
                    similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :fuzzyCandidateCap
            ),
            candidates AS (
                SELECT id FROM fts_hits
                UNION
                SELECT id FROM fuzzy_hits
            )
            SELECT m.*
            FROM messages m
            JOIN candidates c ON c.id = m.id
            CROSS JOIN params p
            ORDER BY
                (
                    CASE
                        WHEN lower(coalesce(m.content, '')) LIKE ('%' || lower(p.raw_q) || '%') THEN 1000
                        ELSE 0
                    END
                    + CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 200
                        ELSE 0
                    END
                    + (ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) * 100)
                    + (similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) * 10)
                ) DESC,
                m.created_at DESC,
                m.id DESC
            """,
        countQuery = """
            WITH params AS (
                SELECT trim(:query) AS raw_q,
                       unaccent_immutable(lower(trim(:query))) AS norm_q,
                       plainto_tsquery('simple', trim(:query)) AS ts_q
            ),
            fts_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id = :channelId
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND to_tsvector('simple', coalesce(m.content, '')) @@ p.ts_q
                ORDER BY
                    ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :ftsCandidateCap
            ),
            fuzzy_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id = :channelId
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND length(p.norm_q) >= :fuzzyMinQueryLength
                  AND (
                        unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%')
                        OR similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) >= :similarityThreshold
                  )
                ORDER BY
                    CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 1
                        ELSE 0
                    END DESC,
                    similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :fuzzyCandidateCap
            )
            SELECT COUNT(DISTINCT id)
            FROM (
                SELECT id FROM fts_hits
                UNION
                SELECT id FROM fuzzy_hits
            ) candidate_ids
            """,
        nativeQuery = true
    )
    Page<Message> searchByChannelIdHybrid(
            @Param("channelId") UUID channelId,
            @Param("query") String query,
            @Param("ftsCandidateCap") int ftsCandidateCap,
            @Param("fuzzyCandidateCap") int fuzzyCandidateCap,
            @Param("fuzzyMinQueryLength") int fuzzyMinQueryLength,
            @Param("similarityThreshold") double similarityThreshold,
            Pageable pageable);

    // ── Search: multi-channel FTS (server-scope / DM-scope) ──────────────────
    @Query(
        value = """
            SELECT * FROM messages
            WHERE channel_id IN :channelIds
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            ORDER BY created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM messages
            WHERE channel_id IN :channelIds
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            """,
        nativeQuery = true
    )
    Page<Message> searchByChannelIdsLegacy(@Param("channelIds") List<UUID> channelIds, @Param("query") String query, Pageable pageable);

    @Query(
        value = """
            WITH params AS (
                SELECT trim(:query) AS raw_q,
                       unaccent_immutable(lower(trim(:query))) AS norm_q,
                       plainto_tsquery('simple', trim(:query)) AS ts_q
            ),
            fts_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id IN :channelIds
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND to_tsvector('simple', coalesce(m.content, '')) @@ p.ts_q
                ORDER BY
                    ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :ftsCandidateCap
            ),
            fuzzy_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id IN :channelIds
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND length(p.norm_q) >= :fuzzyMinQueryLength
                  AND (
                        unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%')
                        OR similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) >= :similarityThreshold
                  )
                ORDER BY
                    CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 1
                        ELSE 0
                    END DESC,
                    similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :fuzzyCandidateCap
            ),
            candidates AS (
                SELECT id FROM fts_hits
                UNION
                SELECT id FROM fuzzy_hits
            )
            SELECT m.*
            FROM messages m
            JOIN candidates c ON c.id = m.id
            CROSS JOIN params p
            ORDER BY
                (
                    CASE
                        WHEN lower(coalesce(m.content, '')) LIKE ('%' || lower(p.raw_q) || '%') THEN 1000
                        ELSE 0
                    END
                    + CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 200
                        ELSE 0
                    END
                    + (ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) * 100)
                    + (similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) * 10)
                ) DESC,
                m.created_at DESC,
                m.id DESC
            """,
        countQuery = """
            WITH params AS (
                SELECT trim(:query) AS raw_q,
                       unaccent_immutable(lower(trim(:query))) AS norm_q,
                       plainto_tsquery('simple', trim(:query)) AS ts_q
            ),
            fts_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id IN :channelIds
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND to_tsvector('simple', coalesce(m.content, '')) @@ p.ts_q
                ORDER BY
                    ts_rank_cd(to_tsvector('simple', coalesce(m.content, '')), p.ts_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :ftsCandidateCap
            ),
            fuzzy_hits AS (
                SELECT m.id
                FROM messages m, params p
                WHERE m.channel_id IN :channelIds
                  AND (m.is_deleted IS NULL OR m.is_deleted = false)
                  AND length(p.norm_q) >= :fuzzyMinQueryLength
                  AND (
                        unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%')
                        OR similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) >= :similarityThreshold
                  )
                ORDER BY
                    CASE
                        WHEN unaccent_immutable(lower(coalesce(m.content, ''))) LIKE ('%' || p.norm_q || '%') THEN 1
                        ELSE 0
                    END DESC,
                    similarity(unaccent_immutable(lower(coalesce(m.content, ''))), p.norm_q) DESC,
                    m.created_at DESC,
                    m.id DESC
                LIMIT :fuzzyCandidateCap
            )
            SELECT COUNT(DISTINCT id)
            FROM (
                SELECT id FROM fts_hits
                UNION
                SELECT id FROM fuzzy_hits
            ) candidate_ids
            """,
        nativeQuery = true
    )
    Page<Message> searchByChannelIdsHybrid(
            @Param("channelIds") List<UUID> channelIds,
            @Param("query") String query,
            @Param("ftsCandidateCap") int ftsCandidateCap,
            @Param("fuzzyCandidateCap") int fuzzyCandidateCap,
            @Param("fuzzyMinQueryLength") int fuzzyMinQueryLength,
            @Param("similarityThreshold") double similarityThreshold,
            Pageable pageable);

    @Query(
        value = """
            SELECT COUNT(*) FROM messages
            WHERE channel_id = :channelId
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            """,
        nativeQuery = true
    )
    long countFtsByChannelId(@Param("channelId") UUID channelId, @Param("query") String query);

    @Query(
        value = """
            SELECT COUNT(*) FROM messages
            WHERE channel_id IN :channelIds
              AND (is_deleted IS NULL OR is_deleted = false)
              AND to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', :query)
            """,
        nativeQuery = true
    )
    long countFtsByChannelIds(@Param("channelIds") List<UUID> channelIds, @Param("query") String query);

    // ── Pinned messages ────────────────────────────────────────────────────────
    @Query("""
            SELECT m FROM Message m
            WHERE m.channelId = :channelId
              AND m.isPinned = true
              AND (m.isDeleted IS NULL OR m.isDeleted = false)
            ORDER BY m.createdAt DESC
            """)
    List<Message> findPinnedByChannelId(@Param("channelId") UUID channelId);

    @Query("""
            SELECT m FROM Message m
            WHERE m.channelId = :channelId
              AND m.isPinned = true
              AND (m.isDeleted IS NULL OR m.isDeleted = false)
              AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.createdAt DESC
            """)
    List<Message> findPinnedByChannelIdContaining(@Param("channelId") UUID channelId, @Param("query") String query);

    @Query("""
            SELECT m FROM Message m
            WHERE m.channelId IN :channelIds
              AND m.isPinned = true
              AND (m.isDeleted IS NULL OR m.isDeleted = false)
            ORDER BY m.createdAt DESC
            """)
    List<Message> findPinnedByChannelIds(@Param("channelIds") List<UUID> channelIds);

    @Query("""
            SELECT m FROM Message m
            WHERE m.channelId IN :channelIds
              AND m.isPinned = true
              AND (m.isDeleted IS NULL OR m.isDeleted = false)
              AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.createdAt DESC
            """)
    List<Message> findPinnedByChannelIdsContaining(@Param("channelIds") List<UUID> channelIds, @Param("query") String query);

    // ── Context window ─────────────────────────────────────────────────────────
    @Query(
        value = """
            (SELECT * FROM messages
             WHERE channel_id = :channelId
               AND (is_deleted IS NULL OR is_deleted = false)
               AND created_at <= (SELECT created_at FROM messages WHERE id = :messageId AND is_deleted = false)
             ORDER BY created_at DESC LIMIT :half)
            UNION ALL
            (SELECT * FROM messages
             WHERE channel_id = :channelId
               AND (is_deleted IS NULL OR is_deleted = false)
               AND created_at > (SELECT created_at FROM messages WHERE id = :messageId AND is_deleted = false)
             ORDER BY created_at ASC LIMIT :half)
            """,
        nativeQuery = true
    )
    List<Message> findMessagesAroundId(@Param("channelId") UUID channelId, @Param("messageId") UUID messageId, @Param("half") int half);

    // ── Cursor-based: messages before a given message ──────────────────────────
    @Query(
        value = """
            SELECT * FROM messages
            WHERE channel_id = :channelId
              AND (is_deleted IS NULL OR is_deleted = false)
              AND created_at < (SELECT created_at FROM messages WHERE id = :beforeId)
            ORDER BY created_at DESC LIMIT :size
            """,
        nativeQuery = true
    )
    List<Message> findMessagesBefore(
            @Param("channelId") UUID channelId,
            @Param("beforeId") UUID beforeId,
            @Param("size") int size
    );

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
