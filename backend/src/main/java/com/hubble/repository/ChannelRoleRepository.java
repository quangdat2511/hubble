package com.hubble.repository;

import com.hubble.entity.ChannelRole;
import com.hubble.entity.ChannelRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChannelRoleRepository extends JpaRepository<ChannelRole, ChannelRoleId> {
    List<ChannelRole> findAllByChannelId(UUID channelId);
    boolean existsByChannelIdAndRoleIdIn(UUID channelId, Collection<UUID> roleIds);
    void deleteByChannelIdAndRoleId(UUID channelId, UUID roleId);
    void deleteAllByChannelId(UUID channelId);
}
