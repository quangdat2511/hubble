package com.hubble.service;

import com.hubble.dto.response.SharedContentPageResponse;
import com.hubble.entity.Attachment;
import com.hubble.entity.Channel;
import com.hubble.entity.Message;
import com.hubble.enums.ChannelType;
import com.hubble.enums.SharedContentType;
import com.hubble.exception.AppException;
import com.hubble.repository.AttachmentRepository;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    AttachmentRepository attachmentRepository;

    @Mock
    MessageMapper messageMapper;

    @Mock
    ChannelRepository channelRepository;

    @Mock
    ChannelMemberRepository channelMemberRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    MessageService messageService;

    @Test
    void getSharedContentShouldReturnMediaItemsForAccessibleDm() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(Channel.builder()
                .id(channelId)
                .type(ChannelType.DM)
                .build()));
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .messageId(messageId)
                .filename("photo.png")
                .url("https://cdn.example.com/photo.png")
                .contentType("image/png")
                .sizeBytes(1024L)
                .createdAt(LocalDateTime.of(2026, 4, 3, 14, 0))
                .build();

        when(attachmentRepository.findSharedMediaByChannelId(eq(channelId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(attachment), PageRequest.of(0, 24), 1));

        SharedContentPageResponse response = messageService.getSharedContent(
                userId.toString(),
                channelId.toString(),
                SharedContentType.MEDIA,
                0,
                24
        );

        assertThat(response.getType()).isEqualTo("MEDIA");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo(attachmentId.toString());
        assertThat(response.getItems().get(0).getPreviewUrl()).isEqualTo("https://cdn.example.com/photo.png");
        verify(attachmentRepository).findSharedMediaByChannelId(channelId, PageRequest.of(0, 24));
    }

    @Test
    void getSharedContentShouldReturnAudioItemsInFilesForAccessibleDm() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(Channel.builder()
                .id(channelId)
                .type(ChannelType.DM)
                .build()));
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .messageId(messageId)
                .filename("voice-note.mp3")
                .url("https://cdn.example.com/voice-note.mp3")
                .contentType("audio/mpeg")
                .sizeBytes(2048L)
                .createdAt(LocalDateTime.of(2026, 4, 3, 14, 30))
                .build();

        when(attachmentRepository.findSharedFilesByChannelId(eq(channelId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(attachment), PageRequest.of(0, 24), 1));

        SharedContentPageResponse response = messageService.getSharedContent(
                userId.toString(),
                channelId.toString(),
                SharedContentType.FILE,
                0,
                24
        );

        assertThat(response.getType()).isEqualTo("FILE");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo(attachmentId.toString());
        assertThat(response.getItems().get(0).getFilename()).isEqualTo("voice-note.mp3");
        assertThat(response.getItems().get(0).getContentType()).isEqualTo("audio/mpeg");
        verify(attachmentRepository).findSharedFilesByChannelId(channelId, PageRequest.of(0, 24));
    }

    @Test
    void getSharedContentShouldExtractNormalizedLinks() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(Channel.builder()
                .id(channelId)
                .type(ChannelType.DM)
                .build()));
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);

        Message message = Message.builder()
                .id(messageId)
                .channelId(channelId)
                .authorId(userId)
                .content("Visit https://example.com/path and www.hubble.test now.")
                .createdAt(LocalDateTime.of(2026, 4, 3, 15, 30))
                .build();

        when(messageRepository.findSharedLinkMessagesByChannelId(eq(channelId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(message), PageRequest.of(0, 24), 1));

        SharedContentPageResponse response = messageService.getSharedContent(
                userId.toString(),
                channelId.toString(),
                SharedContentType.LINK,
                0,
                24
        );

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getUrl()).isEqualTo("https://example.com/path");
        assertThat(response.getItems().get(1).getUrl()).isEqualTo("https://www.hubble.test");
    }

    @Test
    void getSharedContentShouldRejectNonDmChannels() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(Channel.builder()
                .id(channelId)
                .type(ChannelType.TEXT)
                .build()));
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);

        assertThatThrownBy(() -> messageService.getSharedContent(
                userId.toString(),
                channelId.toString(),
                SharedContentType.MEDIA,
                0,
                24
        )).isInstanceOf(AppException.class);
    }

    @Test
    void extractLinksShouldTrimTrailingPunctuationAndDeduplicate() {
        List<String> links = MessageService.extractLinks(
                "Open https://example.com/test, then https://example.com/test and www.hubble.dev!"
        );

        assertThat(links).containsExactly("https://example.com/test", "https://www.hubble.dev");
    }
}
