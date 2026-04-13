package com.hubble.repository;

import com.hubble.entity.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByMessageId(UUID messageId);
    List<Attachment> findByMessageIdIn(List<UUID> messageIds);

    @Query("""
            select a from Attachment a
            where a.messageId in (
                select m.id from Message m
                where m.channelId = :channelId
                  and coalesce(m.isDeleted, false) = false
            )
              and (
                lower(coalesce(a.contentType, '')) like 'image/%'
                or lower(coalesce(a.contentType, '')) like 'video/%'
              )
            order by a.createdAt desc
            """)
    Page<Attachment> findSharedMediaByChannelId(@Param("channelId") UUID channelId, Pageable pageable);

    @Query("""
            select a from Attachment a
            where a.messageId in (
                select m.id from Message m
                where m.channelId = :channelId
                  and coalesce(m.isDeleted, false) = false
            )
              and (
                a.contentType is null
                or (
                    lower(a.contentType) not like 'image/%'
                    and lower(a.contentType) not like 'video/%'
                )
              )
            order by a.createdAt desc
            """)
    Page<Attachment> findSharedFilesByChannelId(@Param("channelId") UUID channelId, Pageable pageable);
}
