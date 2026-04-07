package com.example.hubble.view.dm;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.SharedContentItemResponse;

import java.net.URI;
import java.util.Locale;

public final class DmOverviewItem {

    public enum Kind {
        IMAGE,
        VIDEO,
        LINK,
        FILE
    }

    private final String stableId;
    private final String messageId;
    private final Kind kind;
    private final String title;
    private final String supportingText;
    private final String url;
    private final String previewUrl;
    private final String contentType;
    private final long sizeBytes;
    private final String createdAt;

    private DmOverviewItem(
            @NonNull String stableId,
            @Nullable String messageId,
            @NonNull Kind kind,
            @NonNull String title,
            @Nullable String supportingText,
            @Nullable String url,
            @Nullable String previewUrl,
            @Nullable String contentType,
            long sizeBytes,
            @Nullable String createdAt
    ) {
        this.stableId = stableId;
        this.messageId = messageId;
        this.kind = kind;
        this.title = title;
        this.supportingText = supportingText;
        this.url = url;
        this.previewUrl = previewUrl;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    @NonNull
    public static DmOverviewItem fromSharedContent(@NonNull Context context, @NonNull SharedContentItemResponse item) {
        Kind kind = classifyAttachment(item.getContentType(), item.getType());
        String safeUrl = NetworkConfig.resolveUrl(item.getUrl());
        String safePreviewUrl = NetworkConfig.resolveUrl(item.getPreviewUrl());
        String title = firstNonBlank(
                item.getResolvedFileName(),
                extractHost(safeUrl),
                context.getString(R.string.dm_gallery_fallback_shared_item)
        );
        String supportingText;
        if (kind == Kind.LINK) {
            supportingText = firstNonBlank(item.getMessageContent(), item.getUrl());
        } else if (kind == Kind.FILE) {
            supportingText = buildFileMeta(item.getContentType(), item.getSizeBytes());
        } else {
            supportingText = item.getMessageContent();
        }

        return new DmOverviewItem(
                buildStableId("shared", item.getId(), item.getMessageId(), title),
                item.getMessageId(),
                kind,
                title,
                supportingText,
                safeUrl,
                safePreviewUrl,
                item.getContentType(),
                item.getSizeBytes(),
                item.getCreatedAt()
        );
    }

    @NonNull
    public String getStableId() {
        return stableId;
    }

    @Nullable
    public String getMessageId() {
        return messageId;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getSupportingText() {
        return supportingText;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getPreviewUrl() {
        return TextUtils.isEmpty(previewUrl) ? url : previewUrl;
    }

    @Nullable
    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isMedia() {
        return kind == Kind.IMAGE || kind == Kind.VIDEO;
    }

    public boolean isVideo() {
        return kind == Kind.VIDEO;
    }

    public boolean isLink() {
        return kind == Kind.LINK;
    }

    public boolean isFile() {
        return kind == Kind.FILE;
    }

    public boolean isOpenable() {
        return !TextUtils.isEmpty(url);
    }

    public boolean isDownloadable() {
        return !isLink() && !TextUtils.isEmpty(url);
    }

    @NonNull
    private static Kind classifyAttachment(@Nullable String contentType, @Nullable String fallbackType) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.US);
        String normalizedFallbackType = fallbackType == null ? "" : fallbackType.trim().toLowerCase(Locale.US);

        if (normalizedFallbackType.equals("link")) {
            return Kind.LINK;
        }
        if (normalizedFallbackType.equals("file")) {
            return Kind.FILE;
        }
        if (normalizedContentType.startsWith("video/")) {
            return Kind.VIDEO;
        }
        if (normalizedContentType.startsWith("image/")) {
            return Kind.IMAGE;
        }
        if (normalizedContentType.startsWith("text/html")) {
            return Kind.LINK;
        }
        if (normalizedFallbackType.equals("media")) {
            return Kind.IMAGE;
        }
        return Kind.FILE;
    }

    @NonNull
    private static String buildFileMeta(@Nullable String contentType, long sizeBytes) {
        String sizeLabel = formatFileSize(sizeBytes);
        if (TextUtils.isEmpty(contentType)) {
            return sizeLabel;
        }
        return contentType + " • " + sizeLabel;
    }

    @NonNull
    private static String formatFileSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            return "Unknown size";
        }
        double size = sizeBytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (size >= 1024d && unitIndex < units.length - 1) {
            size /= 1024d;
            unitIndex++;
        }
        return String.format(Locale.US, size >= 10 ? "%.0f %s" : "%.1f %s", size, units[unitIndex]);
    }

    @Nullable
    private static String extractHost(@Nullable String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(URLUtil.guessUrl(rawUrl));
            return uri.getHost();
        } catch (Exception ignored) {
            try {
                Uri uri = Uri.parse(rawUrl);
                return uri.getHost();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    @NonNull
    private static String buildStableId(String prefix, String... parts) {
        StringBuilder builder = new StringBuilder(prefix);
        if (parts != null) {
            for (String part : parts) {
                if (!TextUtils.isEmpty(part)) {
                    builder.append(':').append(part);
                }
            }
        }
        return builder.toString();
    }

    @NonNull
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
