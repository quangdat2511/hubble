package com.example.hubble.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkExtractionUtils {

    private static final Pattern LINK_PATTERN =
            Pattern.compile("((?:https?://|www\\.)[^\\s<>()]+)", Pattern.CASE_INSENSITIVE);

    private LinkExtractionUtils() {
    }

    @NonNull
    public static List<String> extractLinks(@Nullable String content) {
        if (TextUtils.isEmpty(content) || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> links = new LinkedHashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            String match = trimTrailingPunctuation(matcher.group(1));
            if (!match.isEmpty()) {
                links.add(match.startsWith("http://") || match.startsWith("https://")
                        ? match
                        : "https://" + match);
            }
        }
        return new ArrayList<>(links);
    }

    @NonNull
    private static String trimTrailingPunctuation(@Nullable String value) {
        if (value == null) {
            return "";
        }

        int end = value.length();
        while (end > 0) {
            char ch = value.charAt(end - 1);
            if (".,!?;:)]}".indexOf(ch) >= 0) {
                end--;
                continue;
            }
            break;
        }
        return value.substring(0, end).trim();
    }
}
