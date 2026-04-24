package com.example.hubble.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.data.model.dm.ChannelDto;

public final class ServerChannelNameFormatter {

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
        // Keep dynamic names exactly as stored in DB.
        return rawName;
    }
}
