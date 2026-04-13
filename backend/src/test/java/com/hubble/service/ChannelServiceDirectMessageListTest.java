package com.hubble.service;

import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.RoleRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import com.hubble.repository.UserRepository;
import com.hubble.mapper.ChannelMapper;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Danh sách hội thoại DM (tin nhắn gần đây theo kênh) — REST {@code GET /api/channels/dm}.
 */
@ExtendWith(MockitoExtension.class)
class ChannelServiceDirectMessageListTest {

    @Mock ChannelMapper channelMapper;
    @Mock UserRepository userRepository;
    @Mock ChannelRepository channelRepository;
    @Mock ChannelMemberRepository channelMemberRepository;
    @Mock MessageRepository messageRepository;
    @Mock ChannelRoleRepository channelRoleRepository;
    @Mock ServerMemberRepository serverMemberRepository;
    @Mock ServerRepository serverRepository;
    @Mock RoleRepository roleRepository;
    @Mock MessageService messageService;
    @Spy
    SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();

    @InjectMocks ChannelService channelService;

    @Test
    void getDirectChannels_noMemberships_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(channelMemberRepository.findAllByUserId(userId)).thenReturn(List.of());

        assertTrue(channelService.getDirectChannels(userId).isEmpty());
    }
}
