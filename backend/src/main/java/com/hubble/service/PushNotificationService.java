package com.hubble.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.hubble.entity.DeviceToken;
import com.hubble.repository.DeviceTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PushNotificationService {

    DeviceTokenRepository deviceTokenRepository;

    public void sendPushNotification(UUID userId, String title, String body) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.warn("No device tokens found for user {} to send notification", userId);
            return;
        }

        log.info("Sending push notification to user {} with {} tokens. Title: {}, Body: {}", userId, tokens.size(), title, body);

        for (DeviceToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("title", title)
                        .putData("body", body)
                        .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                        .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                                .setNotification(com.google.firebase.messaging.AndroidNotification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .setSound("default")
                                        .build())
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent FCM message to user {}: {}", userId, response);
            } catch (Exception e) {
                log.error("Error sending FCM to user {} with token {}: {}", userId, token.getToken(), e.getMessage(), e);
            }
        }
    }
}