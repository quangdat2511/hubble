package com.example.hubble.utils;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.hubble.R;

import java.util.Locale;

public final class UserStatusFormatter {

    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_DND = "DND";

    private UserStatusFormatter() {
    }

    @NonNull
    public static String normalize(@Nullable String status) {
        if (TextUtils.isEmpty(status)) {
            return STATUS_ONLINE;
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        switch (normalizedStatus) {
            case STATUS_IDLE:
                return STATUS_IDLE;
            case STATUS_OFFLINE:
                return STATUS_OFFLINE;
            case STATUS_DND:
                return STATUS_DND;
            default:
                return STATUS_ONLINE;
        }
    }

    @NonNull
    public static String getDisplayLabel(@NonNull Context context, @Nullable String status) {
        return context.getString(getStatusLabelRes(normalize(status)));
    }

    @NonNull
    public static String[] getDisplayLabels(@NonNull Context context) {
        return new String[] {
                getDisplayLabel(context, STATUS_ONLINE),
                getDisplayLabel(context, STATUS_IDLE),
                getDisplayLabel(context, STATUS_OFFLINE),
                getDisplayLabel(context, STATUS_DND)
        };
    }

    @NonNull
    public static String getCodeFromDisplayLabel(@NonNull Context context, @Nullable String label) {
        if (TextUtils.isEmpty(label)) {
            return STATUS_ONLINE;
        }

        String trimmedLabel = label.trim();
        for (String status : new String[] {STATUS_ONLINE, STATUS_IDLE, STATUS_OFFLINE, STATUS_DND}) {
            if (getDisplayLabel(context, status).equalsIgnoreCase(trimmedLabel)) {
                return status;
            }
        }

        return normalize(trimmedLabel);
    }

    @StringRes
    private static int getStatusLabelRes(@NonNull String status) {
        switch (status) {
            case STATUS_IDLE:
                return R.string.status_idle;
            case STATUS_OFFLINE:
                return R.string.status_offline;
            case STATUS_DND:
                return R.string.status_dnd;
            default:
                return R.string.status_online;
        }
    }
}
