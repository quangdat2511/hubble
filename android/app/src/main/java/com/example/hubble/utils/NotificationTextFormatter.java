package com.example.hubble.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.model.notify.NotificationResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NotificationTextFormatter {

    private static final String FRIEND_REQUEST_VI_SUFFIX = " đã gửi cho bạn lời mời kết bạn.";
    private static final String FRIEND_REQUEST_EN_SUFFIX = " sent you a friend request.";
    private static final String FRIEND_ACCEPTED_VI_SUFFIX = " đã chấp nhận lời mời kết bạn của bạn.";
    private static final String FRIEND_ACCEPTED_EN_SUFFIX = " accepted your friend request.";

    private static final Pattern SERVER_INVITE_VI_PATTERN =
            Pattern.compile("^(.+?) đã mời bạn tham gia (.+)\\.$");
    private static final Pattern SERVER_INVITE_EN_PATTERN =
            Pattern.compile("^(.+?) invited you to join (.+)\\.$");
    private static final Pattern DEVICE_ALERT_VI_PATTERN =
            Pattern.compile("^Phát hiện đăng nhập trên thiết bị mới: (.+?) \\((.+?)\\)\\. Nếu không phải bạn, hãy đổi mật khẩu ngay\\.$");
    private static final Pattern DEVICE_ALERT_EN_PATTERN =
            Pattern.compile("^New login detected on (.+?) \\((.+?)\\)\\. If this wasn't you, change your password now\\.$");

    private NotificationTextFormatter() {
    }

    @NonNull
    public static String format(@NonNull Context context, @Nullable NotificationResponse notification) {
        if (notification == null || notification.getContent() == null || notification.getContent().isEmpty()) {
            return "";
        }

        String content = notification.getContent();
        String type = notification.getType();

        if ("FRIEND_REQUEST".equals(type)) {
            String localized = formatFriendRequest(context, content);
            if (localized != null) {
                return localized;
            }
        } else if ("SERVER_INVITE".equals(type)) {
            String localized = formatServerInvite(context, content);
            if (localized != null) {
                return localized;
            }
        } else if ("SYSTEM_ALERT".equals(type)) {
            String localized = formatSystemAlert(context, content);
            if (localized != null) {
                return localized;
            }
        }

        return content;
    }

    @Nullable
    private static String formatFriendRequest(@NonNull Context context, @NonNull String content) {
        String senderName = extractPrefix(content, FRIEND_REQUEST_VI_SUFFIX);
        if (senderName == null) {
            senderName = extractPrefix(content, FRIEND_REQUEST_EN_SUFFIX);
        }
        if (senderName != null) {
            return context.getString(R.string.notification_friend_request_text, senderName);
        }

        String accepterName = extractPrefix(content, FRIEND_ACCEPTED_VI_SUFFIX);
        if (accepterName == null) {
            accepterName = extractPrefix(content, FRIEND_ACCEPTED_EN_SUFFIX);
        }
        if (accepterName != null) {
            return context.getString(R.string.notification_friend_accepted_text, accepterName);
        }

        return null;
    }

    @Nullable
    private static String formatServerInvite(@NonNull Context context, @NonNull String content) {
        Matcher vietnameseMatcher = SERVER_INVITE_VI_PATTERN.matcher(content);
        if (vietnameseMatcher.matches()) {
            return context.getString(
                    R.string.notification_server_invite_text,
                    vietnameseMatcher.group(1),
                    vietnameseMatcher.group(2)
            );
        }

        Matcher englishMatcher = SERVER_INVITE_EN_PATTERN.matcher(content);
        if (englishMatcher.matches()) {
            return context.getString(
                    R.string.notification_server_invite_text,
                    englishMatcher.group(1),
                    englishMatcher.group(2)
            );
        }

        return null;
    }

    @Nullable
    private static String formatSystemAlert(@NonNull Context context, @NonNull String content) {
        Matcher vietnameseMatcher = DEVICE_ALERT_VI_PATTERN.matcher(content);
        if (vietnameseMatcher.matches()) {
            return context.getString(
                    R.string.notification_new_device_alert_text,
                    vietnameseMatcher.group(1),
                    vietnameseMatcher.group(2)
            );
        }

        Matcher englishMatcher = DEVICE_ALERT_EN_PATTERN.matcher(content);
        if (englishMatcher.matches()) {
            return context.getString(
                    R.string.notification_new_device_alert_text,
                    englishMatcher.group(1),
                    englishMatcher.group(2)
            );
        }

        return null;
    }

    @Nullable
    private static String extractPrefix(@NonNull String content, @NonNull String suffix) {
        if (!content.endsWith(suffix)) {
            return null;
        }

        String value = content.substring(0, content.length() - suffix.length()).trim();
        return value.isEmpty() ? null : value;
    }
}
