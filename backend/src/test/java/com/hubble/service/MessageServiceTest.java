package com.hubble.service;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.UpdateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Channel;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.Message;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ChannelRoleRepository;
import com.hubble.repository.MemberRoleRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.UserRepository;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock MessageMapper messageMapper;
    @Mock ChannelRepository channelRepository;
    @Mock ChannelMemberRepository channelMemberRepository;
    @Mock ChannelRoleRepository channelRoleRepository;
    @Mock ServerMemberRepository serverMemberRepository;
    @Mock MemberRoleRepository memberRoleRepository;
    @Mock UserRepository userRepository;
    @Spy
    SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();
    @Mock SmartReplyService smartReplyService;
    @Mock ReactionService reactionService;

    @InjectMocks MessageService messageService;

    UUID channelId;
    UUID authorId;
    UUID otherUserId;
    Channel dmChannel;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        lenient().when(userRepository.findAllById(anyList())).thenReturn(List.of());

        channelId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        dmChannel = Channel.builder()
                .id(channelId)
                .type(ChannelType.DM)
                .serverId(null)
                .isPrivate(true)
                .build();
    }

    @Test
    void getMessages_returnsRecentPageOrderedByRepository() {
        UUID userId = authorId;
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));

        Message m1 = Message.builder().id(UUID.randomUUID()).channelId(channelId).authorId(userId).content("b").build();
        Message m2 = Message.builder().id(UUID.randomUUID()).channelId(channelId).authorId(userId).content("a").build();
        when(messageRepository.findByChannelIdOrderByCreatedAtDesc(eq(channelId), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(m1, m2)));

        when(attachmentRepository.findByMessageIdIn(anyList())).thenReturn(List.of());
        when(reactionService.getReactionsForMessages(anyList())).thenReturn(Map.of());

        MessageResponse r1 = MessageResponse.builder().id(m1.getId().toString()).content("b").build();
        MessageResponse r2 = MessageResponse.builder().id(m2.getId().toString()).content("a").build();
        when(messageMapper.toMessageResponse(m1)).thenReturn(r1);
        when(messageMapper.toMessageResponse(m2)).thenReturn(r2);

        List<MessageResponse> result = messageService.getMessages(channelId.toString(), userId.toString(), 0, 20);

        assertEquals(2, result.size());
        assertEquals("b", result.get(0).getContent());
        assertEquals("a", result.get(1).getContent());
        verify(messageRepository).findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(0, 20));
    }

    @Test
    void getMessages_emptyChannel_returnsEmptyList() {
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));
        when(messageRepository.findByChannelIdOrderByCreatedAtDesc(eq(channelId), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(messageService.getMessages(channelId.toString(), authorId.toString(), 0, 50).isEmpty());
    }

    @Test
    void sendMessage_persistsReplyToId_andBroadcastsToChannelTopic() {
        UUID replyTo = UUID.randomUUID();
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));

        UUID savedId = UUID.randomUUID();
        when(messageRepository.save(argThat(msg ->
                msg.getReplyToId() != null && msg.getReplyToId().equals(replyTo)
                        && msg.getContent().contains("trích dẫn"))))
                .thenAnswer(inv -> {
                    Message m = inv.getArgument(0);
                    m.setId(savedId);
                    m.setCreatedAt(LocalDateTime.now());
                    return m;
                });

        when(attachmentRepository.findByMessageId(savedId)).thenReturn(List.of());
        when(reactionService.getReactionsForMessage(savedId)).thenReturn(List.of());
        MessageResponse mapped = MessageResponse.builder()
                .id(savedId.toString())
                .channelId(channelId.toString())
                .replyToId(replyTo.toString())
                .content("> trích dẫn\nTrả lời ngắn")
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(mapped);

        when(channelMemberRepository.findAllByChannelId(channelId)).thenReturn(List.of(
                ChannelMember.builder().channelId(channelId).userId(authorId).build(),
                ChannelMember.builder().channelId(channelId).userId(otherUserId).build()
        ));

        CreateMessageRequest req = CreateMessageRequest.builder()
                .channelId(channelId.toString())
                .replyToId(replyTo.toString())
                .content("> trích dẫn\nTrả lời ngắn")
                .type("TEXT")
                .build();

        when(smartReplyService.generateSuggestions(anyString())).thenReturn(null);

        MessageResponse out = messageService.sendMessage(authorId.toString(), req);

        assertEquals(replyTo.toString(), out.getReplyToId());
        verify(messagingTemplate).convertAndSend(eq("/topic/channels/" + channelId), any(MessageResponse.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/users/" + otherUserId + "/dm-delivery"),
                any(Object.class)
        );
        verify(messagingTemplate, never()).convertAndSend(contains("/topic/users/" + authorId), any(Object.class));
    }

    @Test
    void sendMessage_unicodeEmojiAndStickerStyleContent_acceptedAsPlainText() {
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(attachmentRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
        when(reactionService.getReactionsForMessage(any(UUID.class))).thenReturn(List.of());
        String stickerPayload = "{\"pack\":\"cats\",\"id\":\"wave\"}";
        when(messageMapper.toMessageResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return MessageResponse.builder()
                    .id(m.getId().toString())
                    .channelId(channelId.toString())
                    .content(m.getContent())
                    .build();
        });
        when(channelMemberRepository.findAllByChannelId(channelId)).thenReturn(List.of());

        String emoji = "Xin chào \uD83D\uDE00 \uD83D\uDC4B";
        CreateMessageRequest req = CreateMessageRequest.builder()
                .channelId(channelId.toString())
                .content(emoji)
                .type("STICKER")
                .build();
        assertTrue(messageService.sendMessage(authorId.toString(), req).getContent().contains("\uD83D\uDE00"));

        CreateMessageRequest giphy = CreateMessageRequest.builder()
                .channelId(channelId.toString())
                .content(stickerPayload)
                .type("GIPHY")
                .build();
        assertEquals(stickerPayload, messageService.sendMessage(authorId.toString(), giphy).getContent());
    }

    @Test
    void sendMessage_withAttachments_linksRowsToMessage() {
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));
        UUID attId = UUID.randomUUID();
        Attachment loose = Attachment.builder()
                .id(attId)
                .messageId(null)
                .filename("sticker.webp")
                .url("https://media.giphy.com/media/x/sticker.webp")
                .build();

        UUID mid = UUID.randomUUID();
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(mid);
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(attachmentRepository.findAllById(List.of(attId))).thenReturn(List.of(loose));
        when(attachmentRepository.findByMessageId(mid)).thenReturn(List.of(
                Attachment.builder().id(attId).messageId(mid).filename("sticker.webp").url(loose.getUrl()).build()
        ));
        when(reactionService.getReactionsForMessage(mid)).thenReturn(List.of());
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(
                MessageResponse.builder().id(mid.toString()).attachments(List.of()).build()
        );
        when(channelMemberRepository.findAllByChannelId(channelId)).thenReturn(List.of());

        CreateMessageRequest req = CreateMessageRequest.builder()
                .channelId(channelId.toString())
                .content("")
                .type("IMAGE")
                .attachmentIds(List.of(attId))
                .build();

        messageService.sendMessage(authorId.toString(), req);

        ArgumentCaptor<List<Attachment>> captor = ArgumentCaptor.forClass(List.class);
        verify(attachmentRepository).saveAll(captor.capture());
        assertEquals(mid, captor.getValue().get(0).getMessageId());
    }

    @Test
    void sendMessage_forwardStyleBody_persistedVerbatim() {
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(dmChannel));
        UUID mid = UUID.randomUUID();
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(mid);
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(attachmentRepository.findByMessageId(mid)).thenReturn(List.of());
        when(reactionService.getReactionsForMessage(mid)).thenReturn(List.of());
        when(messageMapper.toMessageResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return MessageResponse.builder().id(mid.toString()).content(m.getContent()).build();
        });
        when(channelMemberRepository.findAllByChannelId(channelId)).thenReturn(List.of());

        String forwarded = "─── Chuyển tiếp từ A ───\nNội dung gốc\nDòng 2";
        CreateMessageRequest req = CreateMessageRequest.builder()
                .channelId(channelId.toString())
                .content(forwarded)
                .build();

        assertEquals(forwarded, messageService.sendMessage(authorId.toString(), req).getContent());
    }

    @Test
    void editMessage_ownerUpdatesContent_setsEditedAtAndBroadcasts() {
        UUID mid = UUID.randomUUID();
        Message existing = Message.builder()
                .id(mid)
                .channelId(channelId)
                .authorId(authorId)
                .content("old")
                .isDeleted(false)
                .build();
        when(messageRepository.findById(mid)).thenReturn(Optional.of(existing));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByMessageId(mid)).thenReturn(List.of());
        when(reactionService.getReactionsForMessage(mid)).thenReturn(List.of());
        when(messageMapper.toMessageResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return MessageResponse.builder().id(mid.toString()).content(m.getContent()).editedAt("set").build();
        });

        UpdateMessageRequest update = new UpdateMessageRequest();
        update.setContent("  mới  ");
        MessageResponse res = messageService.editMessage(authorId.toString(), mid.toString(), update);

        assertEquals("mới", res.getContent());
        assertNotNull(existing.getEditedAt());
        verify(messagingTemplate).convertAndSend(eq("/topic/channels/" + channelId), any(MessageResponse.class));
    }

    @Test
    void editMessage_notOwner_throws() {
        UUID mid = UUID.randomUUID();
        when(messageRepository.findById(mid)).thenReturn(Optional.of(
                Message.builder().id(mid).channelId(channelId).authorId(otherUserId).content("x").build()
        ));
        UpdateMessageRequest upd = new UpdateMessageRequest();
        upd.setContent("y");
        AppException ex = assertThrows(AppException.class, () ->
                messageService.editMessage(authorId.toString(), mid.toString(), upd));
        assertEquals(ErrorCode.MESSAGE_NOT_OWNER, ex.getErrorCode());
    }

    @Test
    void unsendMessage_softDeletes_replacesContentAndBroadcasts() {
        UUID mid = UUID.randomUUID();
        Message existing = Message.builder()
                .id(mid)
                .channelId(channelId)
                .authorId(authorId)
                .content("secret")
                .isDeleted(false)
                .build();
        when(messageRepository.findById(mid)).thenReturn(Optional.of(existing));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByMessageId(mid)).thenReturn(List.of());
        when(reactionService.getReactionsForMessage(mid)).thenReturn(List.of());
        when(messageMapper.toMessageResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return MessageResponse.builder()
                    .id(mid.toString())
                    .content(m.getContent())
                    .isDeleted(m.getIsDeleted())
                    .build();
        });

        MessageResponse res = messageService.unsendMessage(authorId.toString(), mid.toString());

        assertTrue(Boolean.TRUE.equals(res.getIsDeleted()));
        assertEquals("Tin nhắn đã được thu hồi", res.getContent());
        verify(messagingTemplate).convertAndSend(eq("/topic/channels/" + channelId), any(MessageResponse.class));
    }
}
