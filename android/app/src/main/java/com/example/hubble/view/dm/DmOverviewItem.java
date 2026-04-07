package com.example.hubble.view.dm;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.AttachmentResponse;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.SharedContentItemResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public final class DmOverviewItem {

    public enum Kind {
        IMAGE,
        VIDEO,
        LINK,
        FILE,
        TEXT
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
    private final boolean pinned;

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
            @Nullable String createdAt,
            boolean pinned
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
        this.pinned = pinned;
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
                item.getCreatedAt(),
                false
        );
    }

    @NonNull
    public static List<DmOverviewItem> fromPinnedMessage(@NonNull Context context, @NonNull MessageDto message) {
        List<DmOverviewItem> items = new ArrayList<>();
        if (!Boolean.TRUE.equals(message.getIsPinned())) {
            return items;
        }

        List<AttachmentResponse> attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            for (int index = 0; index < attachments.size(); index++) {
                AttachmentResponse attachment = attachments.get(index);
                if (attachment == null) {
                    continue;
                }
                Kind kind = classifyAttachment(attachment.getContentType(), null);
                String safeUrl = NetworkConfig.resolveUrl(attachment.getUrl());
                String title = firstNonBlank(
                        attachment.getFilename(),
                        context.getString(R.string.dm_gallery_fallback_attachment)
                );
                String supportingText = kind == Kind.FILE
                        ? buildFileMeta(attachment.getContentType(), attachment.getSizeBytes())
                        : message.getContent();
                items.add(new DmOverviewItem(
                        buildStableId("pinned-attachment", message.getId(), attachment.getId(), String.valueOf(index)),
                        message.getId(),
                        kind,
                        title,
                        supportingText,
                        safeUrl,
                        safeUrl,
                        attachment.getContentType(),
                        attachment.getSizeBytes(),
                        message.getCreatedAt(),
                        true
                ));
            }
            return items;
        }

        String content = message.getContent();
        if (TextUtils.isEmpty(content)) {
            return items;
        }

        if (DmMessageAdapter.isMedia(content)) {
            String mediaUrl = NetworkConfig.resolveUrl(DmMessageAdapter.extractMediaUrl(content));
            String mediaTitle = firstNonBlank(
                    DmMessageAdapter.extractMediaTitle(content),
                    context.getString(R.string.dm_gallery_fallback_shared_media)
            );
            items.add(new DmOverviewItem(
                    buildStableId("pinned-media", message.getId(), mediaTitle),
                    message.getId(),
                    Kind.IMAGE,
                    mediaTitle,
                    content,
                    mediaUrl,
                    mediaUrl,
                    "image/*",
                    0L,
                    message.getCreatedAt(),
                    true
            ));
            return items;
        }

        String firstUrl = extractFirstUrl(content);
        if (!TextUtils.isEmpty(firstUrl)) {
            String safeUrl = NetworkConfig.resolveUrl(firstUrl);
            items.add(new DmOverviewItem(
                    buildStableId("pinned-link", message.getId(), safeUrl),
                    message.getId(),
                    Kind.LINK,
                    firstNonBlank(
                            extractHost(safeUrl),
                            safeUrl,
                            context.getString(R.string.dm_gallery_fallback_shared_link)
                    ),
                    content,
                    safeUrl,
                    safeUrl,
                    "text/html",
                    0L,
                    message.getCreatedAt(),
                    true
            ));
            return items;
        }

        items.add(new DmOverviewItem(
                buildStableId("pinned-text", message.getId()),
                message.getId(),
                Kind.TEXT,
                context.getString(R.string.dm_gallery_fallback_pinned_message),
                content,
                null,
                null,
                "text/plain",
                0L,
                message.getCreatedAt(),
                true
        ));
        return items;
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

    public boolean isPinned() {
        return pinned;
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

    public boolean isText() {
        return kind == Kind.TEXT;
    }

    public boolean isOpenable() {
        return !TextUtils.isEmpty(url);
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
    private static String extractFirstUrl(@Nullable String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        Matcher matcher = Patterns.WEB_URL.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String match = matcher.group();
        if (TextUtils.isEmpty(match)) {
            return null;
        }
        return URLUtil.guessUrl(match);
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
