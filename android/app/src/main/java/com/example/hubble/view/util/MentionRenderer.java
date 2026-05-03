package com.example.hubble.view.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.example.hubble.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for applying mention highlight spans to message text.
 * Only @username tokens that appear in the provided mentionedUsernames list
 * (and the literal "@everyone") are highlighted.
 */
public final class MentionRenderer {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\S+)");

    private MentionRenderer() {}

    /**
     * Returns a {@link SpannableString} with highlight spans applied to all
     * {@code @username} tokens in {@code text} that are present in
     * {@code mentionedUsernames} or equal to {@code everyone}.
     *
     * @param context           Android context for color resolution
     * @param text              raw message text content
     * @param mentionedUsernames resolved usernames of mentioned users (no @ prefix)
     * @return SpannableString with mention spans applied, or plain SpannableString if none
     */
    public static SpannableString applyMentionSpans(Context context, String text,
                                                     List<String> mentionedUsernames,
                                                     boolean highlightEveryone) {
        if (text == null || text.isEmpty()) {
            return new SpannableString(text == null ? "" : text);
        }

        SpannableString spannable = new SpannableString(text);

        Set<String> mentionSet = new HashSet<>();
        if (mentionedUsernames != null) {
            mentionSet.addAll(mentionedUsernames);
        }
        if (highlightEveryone) {
            mentionSet.add("everyone");
        }

        int textColor = ContextCompat.getColor(context, R.color.mention_text_color);
        int bgColor = ContextCompat.getColor(context, R.color.mention_bg_color);

        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username != null && mentionSet.contains(username)) {
                int start = matcher.start();
                int end = matcher.end();
                spannable.setSpan(new ForegroundColorSpan(textColor), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new BackgroundColorSpan(bgColor), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return spannable;
    }

    public static SpannableString applyMentionSpans(Context context, String text,
                                                     List<String> mentionedUsernames) {
        return applyMentionSpans(context, text, mentionedUsernames, true);
    }

    /**
     * Convenience overload with an empty mentions list (no highlighting applied
     * except for @everyone).
     */
    public static SpannableString applyMentionSpans(Context context, String text) {
        return applyMentionSpans(context, text, Collections.emptyList(), true);
    }
}
