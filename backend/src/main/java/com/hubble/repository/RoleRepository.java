package com.hubble.repository;

import com.hubble.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByServerIdOrderByPositionDesc(UUID serverId);

    Optional<Role> findByServerIdAndIsDefaultTrue(UUID serverId);

    boolean existsByServerIdAndNameIgnoreCase(UUID serverId, String name);

    int countByServerId(UUID serverId);
}
