package com.example.hubble.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

public final class InAppMessageUtils {

    private InAppMessageUtils() {
    }

    public static void show(@Nullable View anchor, @Nullable CharSequence message) {
        show(anchor, message, Snackbar.LENGTH_SHORT);
    }

    public static void showLong(@Nullable View anchor, @Nullable CharSequence message) {
        show(anchor, message, Snackbar.LENGTH_LONG);
    }

    public static void show(@Nullable Fragment fragment, @Nullable CharSequence message) {
        if (fragment == null) {
            return;
        }
        View anchor = fragment.getView();
        if (anchor == null && fragment.getActivity() != null) {
            anchor = fragment.getActivity().findViewById(android.R.id.content);
        }
        show(anchor, message, Snackbar.LENGTH_SHORT);
    }

    public static void showLong(@Nullable Fragment fragment, @Nullable CharSequence message) {
        if (fragment == null) {
            return;
        }
        View anchor = fragment.getView();
        if (anchor == null && fragment.getActivity() != null) {
            anchor = fragment.getActivity().findViewById(android.R.id.content);
        }
        show(anchor, message, Snackbar.LENGTH_LONG);
    }

    public static void show(@Nullable Context context, @Nullable CharSequence message) {
        show(resolveAnchor(context), message, Snackbar.LENGTH_SHORT);
    }

    public static void showLong(@Nullable Context context, @Nullable CharSequence message) {
        show(resolveAnchor(context), message, Snackbar.LENGTH_LONG);
    }

    private static void show(@Nullable View anchor, @Nullable CharSequence message, int duration) {
        if (anchor == null || message == null) {
            return;
        }

        String trimmedMessage = message.toString().trim();
        if (trimmedMessage.isEmpty()) {
            return;
        }

        Snackbar.make(anchor, trimmedMessage, duration).show();
    }

    @Nullable
    private static View resolveAnchor(@Nullable Context context) {
        Activity activity = unwrapActivity(context);
        return activity != null ? activity.findViewById(android.R.id.content) : null;
    }

    @Nullable
    private static Activity unwrapActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
