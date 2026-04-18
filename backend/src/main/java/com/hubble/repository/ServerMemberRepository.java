package com.hubble.repository;

import com.hubble.entity.ServerMember;
import com.hubble.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, UUID> {
    List<ServerMember> findAllByUserId(UUID userId);
    List<ServerMember> findAllByServerId(UUID serverId);
    Optional<ServerMember> findByServerIdAndUserId(UUID serverId, UUID userId);
    boolean existsByServerIdAndUserId(UUID serverId, UUID userId);

    @Query("""
            SELECT u FROM User u
            JOIN ServerMember sm ON sm.userId = u.id
            WHERE sm.serverId = :serverId
              AND (:q = '' OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(u.username)    LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.displayName ASC, u.username ASC
            """)
    List<User> findUsersByServerIdAndNameContaining(
            @Param("serverId") UUID serverId, @Param("q") String q);
}
