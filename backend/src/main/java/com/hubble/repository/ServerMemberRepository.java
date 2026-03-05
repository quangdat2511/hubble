package com.hubble.repository;

import com.hubble.entity.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, UUID> {
}
