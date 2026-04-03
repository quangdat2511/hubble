package com.hubble.repository;

import com.hubble.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
