package com.hubble.service;

import com.hubble.dto.request.MarkChannelReadRequest;
import com.hubble.dto.response.PeerReadStatusResponse;
import com.hubble.dto.response.ReadReceiptEvent;
import com.hubble.entity.ChannelMember;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.ChannelMemberRepository;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.MessageRepository;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelReadServiceTest {

    @Mock ChannelMemberRepository channelMemberRepository;
    @Mock ChannelRepository channelRepository;
    @Mock MessageRepository messageRepository;
    @Spy
    SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();

    @InjectMocks ChannelReadService channelReadService;

    UUID channelId;
    UUID readerId;
    UUID peerId;
    UUID messageId;
    LocalDateTime msgTime;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        readerId = UUID.randomUUID();
        peerId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        msgTime = LocalDateTime.of(2026, 4, 1, 12, 0);
    }

    @Test
    void markRead_advancesLastReadAndBroadcastsReadReceipt() {
        when(channelRepository.existsById(channelId)).thenReturn(true);
        ChannelMember self = ChannelMember.builder()
                .channelId(channelId)
                .userId(readerId)
                .lastReadAt(null)
                .build();
        when(channelMemberRepository.findByChannelIdAndUserId(channelId, readerId))
                .thenReturn(Optional.of(self));

        Message boundary = Message.builder()
                .id(messageId)
                .channelId(channelId)
                .authorId(peerId)
                .createdAt(msgTime)
                .build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(boundary));

        channelReadService.markRead(readerId, channelId.toString(),
                MarkChannelReadRequest.builder().messageId(messageId.toString()).build());

        assertEquals(msgTime, self.getLastReadAt());
        verify(channelMemberRepository).save(self);
        ArgumentCaptor<ReadReceiptEvent> cap = ArgumentCaptor.forClass(ReadReceiptEvent.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/channels/" + channelId + "/read"),
                cap.capture()
        );
        assertEquals(readerId.toString(), cap.getValue().getUserId());
        assertEquals(messageId.toString(), cap.getValue().getLastReadMessageId());
        assertEquals(msgTime.toString(), cap.getValue().getReadAt());
    }

    @Test
    void markRead_sameOrOlderBoundary_doesNotSaveOrBroadcast() {
        when(channelRepository.existsById(channelId)).thenReturn(true);
        ChannelMember self = ChannelMember.builder()
                .channelId(channelId)
                .userId(readerId)
                .lastReadAt(msgTime)
                .build();
        when(channelMemberRepository.findByChannelIdAndUserId(channelId, readerId))
                .thenReturn(Optional.of(self));

        Message boundary = Message.builder()
                .id(messageId)
                .channelId(channelId)
                .createdAt(msgTime.minusHours(1))
                .build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(boundary));

        channelReadService.markRead(readerId, channelId.toString(),
                MarkChannelReadRequest.builder().messageId(messageId.toString()).build());

        verify(channelMemberRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(contains("/read"), any(Object.class));
    }

    @Test
    void markRead_blankMessageId_throws() {
        AppException ex = assertThrows(AppException.class, () ->
                channelReadService.markRead(readerId, channelId.toString(), new MarkChannelReadRequest()));
        assertEquals(ErrorCode.INVALID_KEY, ex.getErrorCode());
    }

    @Test
    void markRead_messageInOtherChannel_throwsNotFound() {
        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelMemberRepository.findByChannelIdAndUserId(channelId, readerId))
                .thenReturn(Optional.of(ChannelMember.builder().channelId(channelId).userId(readerId).build()));
        Message wrongChannel = Message.builder()
                .id(messageId)
                .channelId(UUID.randomUUID())
                .createdAt(msgTime)
                .build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(wrongChannel));

        AppException ex = assertThrows(AppException.class, () ->
                channelReadService.markRead(readerId, channelId.toString(),
                        MarkChannelReadRequest.builder().messageId(messageId.toString()).build()));
        assertEquals(ErrorCode.MESSAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getPeerReadStatus_returnsPeerLastReadAt() {
        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, readerId)).thenReturn(true);
        when(channelMemberRepository.findAllByChannelId(channelId)).thenReturn(List.of(
                ChannelMember.builder().channelId(channelId).userId(readerId).build(),
                ChannelMember.builder().channelId(channelId).userId(peerId).build()
        ));
        when(channelMemberRepository.findByChannelIdAndUserId(channelId, peerId))
                .thenReturn(Optional.of(ChannelMember.builder()
                        .channelId(channelId)
                        .userId(peerId)
                        .lastReadAt(msgTime)
                        .build()));

        PeerReadStatusResponse res = channelReadService.getPeerReadStatus(readerId, channelId.toString());

        assertEquals(msgTime.toString(), res.getReadAt());
    }

    @Test
    void getPeerReadStatus_notMember_throwsUnauthorized() {
        when(channelRepository.existsById(channelId)).thenReturn(true);
        when(channelMemberRepository.existsByChannelIdAndUserId(channelId, readerId)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                channelReadService.getPeerReadStatus(readerId, channelId.toString()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }
}
