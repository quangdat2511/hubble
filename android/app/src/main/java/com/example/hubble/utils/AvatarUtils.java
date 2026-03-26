package com.example.hubble.utils;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.hubble.data.api.RetrofitClient;

import java.net.URI;

public final class AvatarUtils {

    private AvatarUtils() {
    }

    @NonNull
    public static String resolveAvatarUrl(@Nullable String avatarUrl) {
        if (avatarUrl == null) {
            return "";
        }

        String normalizedAvatarUrl = avatarUrl.trim().replace('\\', '/');
        if (normalizedAvatarUrl.isEmpty()) {
            return "";
        }
        if (normalizedAvatarUrl.startsWith("http://") || normalizedAvatarUrl.startsWith("https://")) {
            return normalizedAvatarUrl;
        }

        String baseOrigin = extractBaseOrigin(RetrofitClient.BASE_URL);
        if (!baseOrigin.isEmpty()) {
            if (normalizedAvatarUrl.startsWith("/")) {
                return baseOrigin + normalizedAvatarUrl;
            }
            if (normalizedAvatarUrl.startsWith("uploads/")) {
                return baseOrigin + "/" + normalizedAvatarUrl;
            }
        }

        String baseUrl = RetrofitClient.BASE_URL.trim();
        if (baseUrl.endsWith("/") && normalizedAvatarUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + normalizedAvatarUrl;
        }
        if (!baseUrl.endsWith("/") && !normalizedAvatarUrl.startsWith("/")) {
            return baseUrl + "/" + normalizedAvatarUrl;
        }
        return baseUrl + normalizedAvatarUrl;
    }

    public static void loadAvatar(@NonNull View viewScope,
                                  @NonNull ImageView imageView,
                                  @Nullable TextView initialsView,
                                  @Nullable String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) {
            showFallback(viewScope, imageView, initialsView);
            return;
        }

        String resolvedAvatarUrl = resolveAvatarUrl(avatarUrl);
        if (resolvedAvatarUrl.isEmpty()) {
            showFallback(viewScope, imageView, initialsView);
            return;
        }

        if (initialsView != null) {
            initialsView.setVisibility(View.GONE);
        }

        Glide.with(viewScope)
                .load(resolvedAvatarUrl)
                .transform(new CircleCrop())
                .error(imageView.getDrawable())
                .into(imageView);
    }

    public static void showFallback(@NonNull View viewScope,
                                    @NonNull ImageView imageView,
                                    @Nullable TextView initialsView) {
        Glide.with(viewScope).clear(imageView);
        if (initialsView != null) {
            initialsView.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    private static String extractBaseOrigin(@NonNull String baseUrl) {
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            String authority = uri.getRawAuthority();
            if (scheme != null && authority != null) {
                return scheme + "://" + authority;
            }
        } catch (IllegalArgumentException ignored) {
        }

        String normalizedBaseUrl = baseUrl.trim().replace('\\', '/');
        if (normalizedBaseUrl.endsWith("/api/")) {
            return normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - "/api/".length());
        }
        if (normalizedBaseUrl.endsWith("/api")) {
            return normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - "/api".length());
        }
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl;
    }
}
