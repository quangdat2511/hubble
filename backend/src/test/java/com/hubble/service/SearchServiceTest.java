package com.hubble.service;

import com.hubble.entity.Channel;
import com.hubble.entity.Message;
import com.hubble.entity.User;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.FriendshipRepository;
import com.hubble.repository.MemberRoleRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServerMemberRepository serverMemberRepository;
    @Mock private MemberRoleRepository memberRoleRepository;
    @Mock private ChannelRoleRepository channelRoleRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchDmMessages_rejectsTooShortQuery() {
        String userId = UUID.randomUUID().toString();

        AppException ex = assertThrows(AppException.class,
                () -> searchService.searchDmMessages(userId, "a", 0, 20));

        assertEquals(ErrorCode.INVALID_KEY, ex.getErrorCode());
    }

    @Test
    void searchDmMessages_returnsOnlyDmMembershipChannels() {
        UUID userId = UUID.randomUUID();
        UUID dmChannelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(channelRepository.findChannelIdsByUserIdAndTypeIn(eq(userId), any()))
                .thenReturn(List.of(dmChannelId));

        Message message = Message.builder()
                .id(messageId)
                .channelId(dmChannelId)
                .authorId(authorId)
                .content("hello dm")
                .createdAt(LocalDateTime.now())
                .build();
        Page<Message> page = new PageImpl<>(List.of(message));
        when(messageRepository.searchByChannelIds(eq(List.of(dmChannelId)), eq("hello"), any(Pageable.class)))
                .thenReturn(page);

        User author = User.builder().id(authorId).username("alice").displayName("Alice").build();
        when(userRepository.findAllById(eq(List.of(authorId)))).thenReturn(List.of(author));

        Page<?> result = searchService.searchDmMessages(userId.toString(), "hello", 0, 20);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchChannelChannels_usesParentServerAndAccessFilter() {
        UUID userId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        UUID sourceChannelId = UUID.randomUUID();
        UUID visibleChannelId = UUID.randomUUID();
        UUID hiddenChannelId = UUID.randomUUID();

        Channel sourceChannel = Channel.builder()
                .id(sourceChannelId)
                .serverId(serverId)
                .isPrivate(false)
                .type(ChannelType.TEXT)
                .name("general")
                .build();
        when(channelRepository.findById(sourceChannelId)).thenReturn(Optional.of(sourceChannel));
        when(serverMemberRepository.existsByServerIdAndUserId(serverId, userId)).thenReturn(true);
        when(channelRepository.findAccessibleChannelIdsByServerId(serverId, userId))
                .thenReturn(List.of(visibleChannelId));

        Channel visible = Channel.builder()
                .id(visibleChannelId)
                .serverId(serverId)
                .name("announcements")
                .type(ChannelType.TEXT)
                .build();
        Channel hidden = Channel.builder()
                .id(hiddenChannelId)
                .serverId(serverId)
                .name("staff-private")
                .type(ChannelType.TEXT)
                .build();
        when(channelRepository.findByServerIdAndNameContainingIgnoreCase(serverId, "ann"))
                .thenReturn(List.of(visible, hidden));

        List<?> result = searchService.searchChannelChannels(userId.toString(), sourceChannelId.toString(), "ann");
        assertEquals(1, result.size());
    }

    @Test
    void searchChannelChannels_returnsEmptyForDmSourceChannel() {
        UUID userId = UUID.randomUUID();
        UUID dmChannelId = UUID.randomUUID();

        Channel dmChannel = Channel.builder()
                .id(dmChannelId)
                .serverId(null)
                .type(ChannelType.DM)
                .build();
        when(channelRepository.findById(dmChannelId)).thenReturn(Optional.of(dmChannel));
        when(channelMemberRepository.existsByChannelIdAndUserId(dmChannelId, userId)).thenReturn(true);

        List<?> result = searchService.searchChannelChannels(userId.toString(), dmChannelId.toString(), "");
        assertTrue(result.isEmpty());
    }
}
