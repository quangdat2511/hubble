package com.hubble.repository;

import com.hubble.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<Server, UUID> {
    boolean existsByInviteCode(String inviteCode);
}
