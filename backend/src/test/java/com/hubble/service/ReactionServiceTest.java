package com.hubble.service;

import com.hubble.dto.response.ReactionResponse;
import com.hubble.entity.MessageReaction;
import com.hubble.repository.MessageReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock MessageReactionRepository reactionRepository;

    @InjectMocks ReactionService reactionService;

    @Test
    void toggleReaction_addsEmojiThenSecondUserIncreasesCount() {
        UUID messageId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        MessageReaction r1 = MessageReaction.builder().messageId(messageId).userId(u1).emoji("👍").build();
        MessageReaction r2 = MessageReaction.builder().messageId(messageId).userId(u2).emoji("👍").build();

        when(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, u1, "👍"))
                .thenReturn(Optional.empty());
        when(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, u2, "👍"))
                .thenReturn(Optional.empty());
        when(reactionRepository.findByMessageId(messageId))
                .thenReturn(List.of(r1))
                .thenReturn(List.of(r1, r2));

        reactionService.toggleReaction(messageId, u1, "👍");
        verify(reactionRepository).save(any(MessageReaction.class));

        List<ReactionResponse> out = reactionService.toggleReaction(messageId, u2, "👍");

        assertEquals(1, out.size());
        assertEquals("👍", out.get(0).getEmoji());
        assertEquals(2, out.get(0).getCount());
        assertTrue(out.get(0).getUserIds().contains(u1.toString()));
        assertTrue(out.get(0).getUserIds().contains(u2.toString()));
    }

    @Test
    void toggleReaction_removesWhenSameUserTogglesAgain() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MessageReaction row = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji("❤️")
                .build();
        when(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, "❤️"))
                .thenReturn(Optional.of(row));
        when(reactionRepository.findByMessageId(messageId)).thenReturn(List.of());

        List<ReactionResponse> out = reactionService.toggleReaction(messageId, userId, "❤️");

        verify(reactionRepository).delete(row);
        assertTrue(out.isEmpty());
    }

    @Test
    void getReactionsForMessages_groupsByMessageId() {
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        UUID u = UUID.randomUUID();
        when(reactionRepository.findByMessageIdIn(List.of(m1, m2))).thenReturn(List.of(
                MessageReaction.builder().messageId(m1).userId(u).emoji("😀").build(),
                MessageReaction.builder().messageId(m2).userId(u).emoji("😀").build()
        ));

        Map<UUID, List<ReactionResponse>> map = reactionService.getReactionsForMessages(List.of(m1, m2));

        assertEquals(1, map.get(m1).get(0).getCount());
        assertEquals(1, map.get(m2).get(0).getCount());
    }

    @Test
    void getReactionsForMessages_emptyInput_returnsEmptyMap() {
        assertTrue(reactionService.getReactionsForMessages(List.of()).isEmpty());
        assertTrue(reactionService.getReactionsForMessages(null).isEmpty());
    }
}
