package com.example.hubble.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.data.api.NetworkConfig;

import java.util.Locale;

public final class MediaDownloadHelper {

    private static final String DOWNLOADS_SUBDIRECTORY = "Hubble";

    private MediaDownloadHelper() {
    }

    @NonNull
    public static EnqueueResult enqueueDownload(
            @NonNull Context context,
            @Nullable String rawUrl,
            @Nullable String suggestedFileName,
            @Nullable String contentType,
            @Nullable String accessToken,
            @NonNull String description
    ) {
        String resolvedUrl = NetworkConfig.resolveUrl(rawUrl);
        if (TextUtils.isEmpty(resolvedUrl)) {
            throw new IllegalArgumentException("Download URL is empty");
        }

        String safeFileName = buildFileName(resolvedUrl, suggestedFileName, contentType);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(resolvedUrl));
        request.setTitle(safeFileName);
        request.setDescription(description);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (!TextUtils.isEmpty(contentType)) {
            request.setMimeType(contentType);
        }
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                DOWNLOADS_SUBDIRECTORY + "/" + safeFileName
        );

        if (!TextUtils.isEmpty(accessToken)) {
            request.addRequestHeader("Authorization", "Bearer " + accessToken);
        }

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            throw new IllegalStateException("Download service unavailable");
        }

        long downloadId = downloadManager.enqueue(request);
        return new EnqueueResult(downloadId, safeFileName);
    }

    @NonNull
    private static String buildFileName(
            @NonNull String resolvedUrl,
            @Nullable String suggestedFileName,
            @Nullable String contentType
    ) {
        String candidate = suggestedFileName;
        if (TextUtils.isEmpty(candidate)) {
            candidate = URLUtil.guessFileName(resolvedUrl, null, contentType);
        }

        candidate = sanitizeFileName(candidate);
        if (!candidate.contains(".")) {
            String extension = inferExtension(contentType, resolvedUrl);
            if (!TextUtils.isEmpty(extension)) {
                candidate = candidate + "." + extension;
            }
        }
        return candidate;
    }

    @NonNull
    private static String sanitizeFileName(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return "download";
        }
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "download" : sanitized;
    }

    @Nullable
    private static String inferExtension(@Nullable String contentType, @NonNull String resolvedUrl) {
        if (!TextUtils.isEmpty(contentType)) {
            String extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(contentType.toLowerCase(Locale.US));
            if (!TextUtils.isEmpty(extension)) {
                return extension;
            }
        }

        String guessed = MimeTypeMap.getFileExtensionFromUrl(resolvedUrl);
        return TextUtils.isEmpty(guessed) ? null : guessed;
    }

    public static final class EnqueueResult {
        private final long downloadId;
        private final String fileName;

        private EnqueueResult(long downloadId, @NonNull String fileName) {
            this.downloadId = downloadId;
            this.fileName = fileName;
        }

        public long getDownloadId() {
            return downloadId;
        }

        @NonNull
        public String getFileName() {
            return fileName;
        }
    }
}
