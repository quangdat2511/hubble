package com.example.hubble.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.model.dm.ChannelDto;

import java.util.Locale;

public final class ServerChannelNameFormatter {

    private static final String DEFAULT_TEXT_CATEGORY_VI = "Kênh chat";
    private static final String DEFAULT_TEXT_CATEGORY_EN = "Text Channels";
    private static final String DEFAULT_TEXT_CHANNEL_VI = "Kênh chung";
    private static final String DEFAULT_TEXT_CHANNEL_EN = "General";
    private static final String DEFAULT_VOICE_CATEGORY_VI = "Kênh đàm thoại";
    private static final String DEFAULT_VOICE_CATEGORY_EN = "Voice Channels";
    private static final String DEFAULT_VOICE_CHANNEL_VI = "Chung";

    private ServerChannelNameFormatter() {
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context, @Nullable ChannelDto channel) {
        if (channel == null) {
            return "";
        }
        return getDisplayName(context, channel.getName(), channel.getType());
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context,
                                        @Nullable String rawName,
                                        @Nullable String channelType) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "";
        }

        String normalizedName = rawName.trim().toLowerCase(Locale.ROOT);
        String normalizedType = channelType == null ? "" : channelType.trim().toUpperCase(Locale.ROOT);

        if ("CATEGORY".equals(normalizedType)) {
            if (matches(normalizedName, DEFAULT_TEXT_CATEGORY_VI, DEFAULT_TEXT_CATEGORY_EN)) {
                return context.getString(R.string.channel_category_text);
            }
            if (matches(normalizedName, DEFAULT_VOICE_CATEGORY_VI, DEFAULT_VOICE_CATEGORY_EN)) {
                return context.getString(R.string.channel_category_voice);
            }
        }

        if ("TEXT".equals(normalizedType)
                && matches(normalizedName, DEFAULT_TEXT_CHANNEL_VI, DEFAULT_TEXT_CHANNEL_EN)) {
            return context.getString(R.string.channel_general);
        }

        if ("VOICE".equals(normalizedType)
                && matches(normalizedName, DEFAULT_VOICE_CHANNEL_VI, DEFAULT_TEXT_CHANNEL_EN)) {
            return context.getString(R.string.channel_general);
        }

        return rawName;
    }

    private static boolean matches(@NonNull String normalizedValue,
                                   @NonNull String firstCandidate,
                                   @NonNull String secondCandidate) {
        return normalizedValue.equals(firstCandidate.toLowerCase(Locale.ROOT))
                || normalizedValue.equals(secondCandidate.toLowerCase(Locale.ROOT));
    }
}
