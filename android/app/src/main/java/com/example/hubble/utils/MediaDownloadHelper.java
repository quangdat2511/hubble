package com.example.hubble.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaDownloadHelper {

    private static final String DOWNLOADS_SUBDIRECTORY = "Hubble";
    private static final Map<Long, String> PENDING_DOWNLOADS = new ConcurrentHashMap<>();
    private static final AtomicBoolean RECEIVER_REGISTERED = new AtomicBoolean(false);
    private static final BroadcastReceiver DOWNLOAD_COMPLETE_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                return;
            }

            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            String fileName = PENDING_DOWNLOADS.remove(downloadId);
            if (downloadId == -1L || TextUtils.isEmpty(fileName)) {
                return;
            }

            DownloadManager downloadManager =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                InAppMessageUtils.showLong(
                        context.getApplicationContext(),
                        context.getString(R.string.dm_gallery_download_failed)
                );
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    return;
                }

                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    InAppMessageUtils.showLong(
                            context.getApplicationContext(),
                            context.getString(R.string.dm_gallery_download_success, fileName)
                    );
                    return;
                }

                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                InAppMessageUtils.showLong(
                        context.getApplicationContext(),
                        context.getString(R.string.dm_gallery_download_failed_with_reason, String.valueOf(reason))
                );
            } catch (Exception e) {
                String message = e.getMessage();
                if (TextUtils.isEmpty(message)) {
                    message = context.getString(R.string.dm_gallery_download_failed);
                }
                InAppMessageUtils.showLong(
                        context.getApplicationContext(),
                        context.getString(R.string.dm_gallery_download_failed_with_reason, message)
                );
            }
        }
    };

    private MediaDownloadHelper() {
    }

    public static void initialize(@NonNull Context context) {
        if (!RECEIVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(
                appContext,
                DOWNLOAD_COMPLETE_RECEIVER,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
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
        initialize(context);

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
        PENDING_DOWNLOADS.put(downloadId, safeFileName);
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
