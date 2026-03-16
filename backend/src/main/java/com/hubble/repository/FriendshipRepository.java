package com.hubble.repository;

import com.hubble.entity.Friendship;
import com.hubble.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);

    @Query("""
            select f from Friendship f
            where (f.requesterId = :userA and f.addresseeId = :userB)
               or (f.requesterId = :userB and f.addresseeId = :userA)
            """)
    Optional<Friendship> findRelationBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);

    List<Friendship> findByAddresseeIdAndStatusOrderByCreatedAtDesc(UUID addresseeId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, FriendshipStatus status);

    @Query("""
            select f from Friendship f
            where f.status = :status
              and (f.requesterId = :userId or f.addresseeId = :userId)
            order by f.createdAt desc
            """)
    List<Friendship> findAcceptedByUserId(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);
}
