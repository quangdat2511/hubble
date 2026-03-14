package com.hubble.service;


import com.hubble.dto.event.MessageEvent;
import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.request.EditMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import com.hubble.enums.MessageType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {
    @Mock
    MessageRepository messageRepository;
    @Mock
    MessageMapper messageMapper;
    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    MessageService messageService;

    UUID channelId;
    UUID messageId;
    UUID authorId;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        authorId  = UUID.randomUUID();
    }

    @Test
    void sendMessage_shouldSaveAndBroadcast() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                            .channelId(channelId)
                            .authorId(authorId)
                            .content("Hubble")
                            .build();
        Message entity = Message.builder()
                .id(messageId)
                .authorId(authorId)
                .channelId(channelId)
                .content("Hubble")
                .type(MessageType.TEXT)
                .build();

        MessageResponse response = MessageResponse.builder()
                .id(messageId)
                .authorId(authorId)
                .channelId(channelId)
                .content("Hubble")
                .type(MessageType.TEXT)
                .build();

        when(messageMapper.toMessage(request)).thenReturn(entity);
        when(messageRepository.save(entity)).thenReturn(entity);
        when(messageMapper.toMessageResponse(entity)).thenReturn(response);

        MessageResponse result = messageService.sendMessage(request);

        assertThat(result.getId()).isEqualTo(messageId);
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getChannelId()).isEqualTo(channelId);
        assertThat(result.getContent()).isEqualTo("Hubble");
        assertThat(result.getType()).isEqualTo(MessageType.TEXT);

        ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/channel/" + channelId),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("SEND");
        assertThat(eventCaptor.getValue().getMessage()).isEqualTo(response);
    }

    @Test
    void updateMessage_shouldUpdateContentAndBroadcast() {
        Message existing = Message.builder()
                .id(messageId)
                .channelId(channelId)
                .content("Old content")
                .build();
        MessageResponse updateResponse = MessageResponse.builder()
                .id(messageId)
                .authorId(authorId)
                .channelId(channelId)
                .content("New content")
                .type(MessageType.TEXT)
                .build();
        EditMessageRequest request = new EditMessageRequest("New content");

        when(messageRepository.findById(messageId)).thenReturn(Optional.ofNullable(existing));
        when(messageRepository.save(existing)).thenReturn(existing);
        when(messageMapper.toMessageResponse(existing)).thenReturn(updateResponse);

        MessageResponse result = messageService.editMessage(messageId, request);

        assertThat(existing.getContent()).isEqualTo("New content");
        assertThat(existing.getEditedAt()).isNotNull();
        assertThat(result.getContent()).isEqualTo("New content");

        // Assert — broadcast đúng action EDIT
        ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/channel/" + channelId),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("EDIT");
    }
    @Test
    void editMessage_whenNotFound_shouldThrowAppException() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.editMessage(messageId, new EditMessageRequest("x"))
        )
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    // ─────────────────────────────────────────────────────────────
    // deleteMessage
    // ─────────────────────────────────────────────────────────────

    @Test
    void deleteMessage_shouldSoftDeleteAndBroadcast() {
        // Arrange
        Message existing = Message.builder()
                .id(messageId)
                .channelId(channelId)
                .isDeleted(false)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existing));
        when(messageRepository.save(existing)).thenReturn(existing);

        // Act
        messageService.deleteMessage(messageId);

        // Assert — isDeleted = true (soft delete, không xóa khỏi DB)
        assertThat(existing.getIsDeleted()).isTrue();

        // Assert — broadcast đúng action DELETE kèm đúng messageId
        ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/channel/" + channelId),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("DELETE");
        assertThat(eventCaptor.getValue().getMessage().getId()).isEqualTo(messageId);
    }

    @Test
    void deleteMessage_whenNotFound_shouldThrowAppException() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.deleteMessage(messageId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);

        verify(messageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }


}
