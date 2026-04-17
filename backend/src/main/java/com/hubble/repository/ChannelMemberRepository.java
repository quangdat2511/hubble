package com.hubble.repository;

import com.hubble.entity.ChannelMember;
import com.hubble.entity.ChannelMemberId;
import com.hubble.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    Optional<ChannelMember> findByChannelIdAndUserId(UUID channelId, UUID userId);

    List<ChannelMember> findAllByUserId(UUID userId);

    List<ChannelMember> findAllByChannelId(UUID channelId);

    void deleteByChannelIdAndUserId(UUID channelId, UUID userId);

    void deleteAllByChannelId(UUID channelId);

    // ── Search: members of a channel matching display name or username ─────────
    @Query("""
            SELECT cm.user FROM ChannelMember cm
            WHERE cm.channelId = :channelId
              AND (LOWER(cm.user.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(cm.user.username) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<User> findUsersByChannelIdAndNameContaining(@Param("channelId") UUID channelId, @Param("query") String query);

    // ── Search: deduplicated users across multiple channels (server-scope) ─────
    @Query("""
            SELECT DISTINCT cm.user FROM ChannelMember cm
            WHERE cm.channelId IN :channelIds
              AND (LOWER(cm.user.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(cm.user.username) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<User> findDistinctUsersByChannelIdsAndNameContaining(@Param("channelIds") List<UUID> channelIds, @Param("query") String query);

    // ── Get channelIds for a user within a specific server ────────────────────
    @Query("""
            SELECT cm.channelId FROM ChannelMember cm
            WHERE cm.userId = :userId AND cm.channel.serverId = :serverId
            """)
    List<UUID> findChannelIdsByUserIdAndServerId(@Param("userId") UUID userId, @Param("serverId") UUID serverId);
}