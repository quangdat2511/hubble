package com.hubble.repository;

import com.hubble.entity.ChannelMember;
import com.hubble.entity.ChannelMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
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
}