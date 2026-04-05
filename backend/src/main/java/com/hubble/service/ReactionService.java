package com.hubble.service;

import com.hubble.dto.response.ReactionResponse;
import com.hubble.entity.MessageReaction;
import com.hubble.repository.MessageReactionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReactionService {

    MessageReactionRepository reactionRepository;

    @Transactional
    public List<ReactionResponse> toggleReaction(UUID messageId, UUID userId, String emoji) {
        Optional<MessageReaction> existing =
                reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            reactionRepository.save(MessageReaction.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .emoji(emoji)
                    .build());
        }

        return getReactionsForMessage(messageId);
    }

    @Transactional(readOnly = true)
    public List<ReactionResponse> getReactionsForMessage(UUID messageId) {
        return buildReactionResponses(reactionRepository.findByMessageId(messageId));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<ReactionResponse>> getReactionsForMessages(List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Map.of();

        List<MessageReaction> allReactions = reactionRepository.findByMessageIdIn(messageIds);

        Map<UUID, List<MessageReaction>> grouped = allReactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getMessageId));

        Map<UUID, List<ReactionResponse>> result = new HashMap<>();
        for (Map.Entry<UUID, List<MessageReaction>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), buildReactionResponses(entry.getValue()));
        }
        return result;
    }

    private List<ReactionResponse> buildReactionResponses(List<MessageReaction> reactions) {
        Map<String, List<MessageReaction>> byEmoji = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji, LinkedHashMap::new, Collectors.toList()));

        List<ReactionResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<MessageReaction>> entry : byEmoji.entrySet()) {
            result.add(ReactionResponse.builder()
                    .emoji(entry.getKey())
                    .count(entry.getValue().size())
                    .userIds(entry.getValue().stream()
                            .map(r -> r.getUserId().toString())
                            .toList())
                    .build());
        }
        return result;
    }
}
