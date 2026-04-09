package com.example.hubble.utils;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;

import com.example.hubble.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class LocalizedTimeUtils {

    private LocalizedTimeUtils() {
    }

    @NonNull
    public static String formatConversationDateLabel(@NonNull Context context, long millis) {
        if (millis < 0) {
            return "";
        }

        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(messageCalendar, today)) {
            return context.getString(R.string.dm_date_today);
        }
        if (isSameDay(messageCalendar, yesterday)) {
            return context.getString(R.string.dm_date_yesterday);
        }

        Locale locale = getAppLocale(context);
        String pattern = DateFormat.getBestDateTimePattern(locale, "d MMMM yyyy");
        return new SimpleDateFormat(pattern, locale).format(new Date(millis));
    }

    @NonNull
    public static String formatRelativeTime(@NonNull Context context, @Nullable String isoDate) {
        Date date = parseIsoDate(isoDate);
        if (date == null) {
            return "";
        }

        long diffMs = Math.max(0L, System.currentTimeMillis() - date.getTime());
        long seconds = diffMs / 1000L;
        if (seconds < 60L) {
            return context.getString(R.string.relative_time_seconds_short, seconds);
        }

        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return context.getString(R.string.relative_time_minutes_short, minutes);
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return context.getString(R.string.relative_time_hours_short, hours);
        }

        long days = hours / 24L;
        if (days < 7L) {
            return context.getString(R.string.relative_time_days_short, days);
        }

        return context.getString(R.string.relative_time_weeks_short, days / 7L);
    }

    @Nullable
    private static Date parseIsoDate(@Nullable String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(isoDate.length() > 19 ? isoDate.substring(0, 19) : isoDate);
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private static Locale getAppLocale(@NonNull Context context) {
        Locale locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
        return locale != null ? locale : Locale.getDefault();
    }

    private static boolean isSameDay(@NonNull Calendar first, @NonNull Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
}
