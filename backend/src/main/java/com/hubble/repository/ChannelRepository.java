package com.hubble.repository;

import com.hubble.entity.Channel;
import com.hubble.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    List<Channel> findByServerId(UUID serverId);
    List<Channel> findByParentId(UUID parentId);

    // ── DM-scope search: get all DM/GROUP_DM channel IDs the user is a member of
    @Query("""
            SELECT cm.channelId FROM ChannelMember cm
            WHERE cm.userId = :userId
              AND cm.channel.type IN :types
            """)
    List<UUID> findChannelIdsByUserIdAndTypeIn(@Param("userId") UUID userId, @Param("types") List<ChannelType> types);

    // ── Server channels accessible to a user (for name-based channel search) ──
    List<Channel> findByServerIdAndNameContainingIgnoreCase(UUID serverId, String name);

    // ── Accessible channel IDs for server scope: public channels + explicit membership ──
    @Query("""
            SELECT c.id FROM Channel c
            WHERE c.serverId = :serverId
              AND (c.isPrivate = false
                   OR EXISTS (SELECT 1 FROM ChannelMember cm
                              WHERE cm.channelId = c.id AND cm.userId = :userId))
            """)
    List<UUID> findAccessibleChannelIdsByServerId(
            @Param("serverId") UUID serverId, @Param("userId") UUID userId);
}
