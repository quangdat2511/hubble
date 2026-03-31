package com.hubble.repository;

import com.hubble.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRole, MemberRole.MemberRoleId> {
    List<MemberRole> findByRoleId(UUID roleId);

    List<MemberRole> findByMemberId(UUID memberId);

    int countByRoleId(UUID roleId);

    void deleteByRoleId(UUID roleId);

    @Query("SELECT mr.roleId FROM MemberRole mr WHERE mr.memberId = :memberId")
    List<UUID> findRoleIdsByMemberId(@Param("memberId") UUID memberId);

    void deleteByMemberIdAndRoleId(UUID memberId, UUID roleId);
}
