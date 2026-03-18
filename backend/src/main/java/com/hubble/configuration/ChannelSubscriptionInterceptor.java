package com.hubble.configuration;

import com.hubble.repository.ChannelMemberRepository;
import com.hubble.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelSubscriptionInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ChannelMemberRepository channelMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Chỉ xử lý SUBSCRIBE (có thể mở rộng thêm SEND nếu client gửi tin trực tiếp qua STOMP)
        if (accessor.getMessageType() == SimpMessageType.SUBSCRIBE
                && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination(); // ví dụ: /topic/channel/{channelId}
            if (destination == null || !destination.startsWith("/topic/channel/")) {
                return message; // không phải subscribe channel chat, bỏ qua
            }

            // 1. Lấy token từ native headers (Authorization: Bearer xxx)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header in WebSocket SUBSCRIBE");
                throw new IllegalArgumentException("Unauthorized: missing token");
            }

            String token = authHeader.substring(7);

            // 2. Lấy userId từ token
            if (!jwtService.validateToken(token)) {
                throw new IllegalArgumentException("Unauthorized: invalid token");
            }
            UUID userId = jwtService.getUserIdFromToken(token);

            // 3. Tách channelId từ destination
            String channelIdStr = destination.substring("/topic/channel/".length());
            UUID channelId;
            try {
                channelId = UUID.fromString(channelIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid channelId in destination: {}", destination);
                throw new IllegalArgumentException("Invalid channel id");
            }

            // 4. Check membership
            boolean isMember = channelMemberRepository.existsByChannelIdAndUserId(channelId, userId);
            if (!isMember) {
                log.warn("User {} tried to subscribe to channel {} without membership", userId, channelId);
                throw new IllegalArgumentException("Forbidden: not a member of this channel");
            }
        }

        return message;
    }
}