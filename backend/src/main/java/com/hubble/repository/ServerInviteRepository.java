package com.hubble.repository;

import com.hubble.entity.ServerInvite;
import com.hubble.enums.ServerInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerInviteRepository extends JpaRepository<ServerInvite, UUID> {

    List<ServerInvite> findAllByServerIdAndStatus(UUID serverId, ServerInviteStatus status);

    List<ServerInvite> findAllByInviteeIdAndStatus(UUID inviteeId, ServerInviteStatus status);

    Optional<ServerInvite> findByServerIdAndInviteeIdAndStatus(UUID serverId, UUID inviteeId, ServerInviteStatus status);

    boolean existsByServerIdAndInviteeIdAndStatus(UUID serverId, UUID inviteeId, ServerInviteStatus status);
}

