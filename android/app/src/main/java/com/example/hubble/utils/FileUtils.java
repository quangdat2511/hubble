package com.example.hubble.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static File getFileFromUri(Context context, Uri uri) {
        try {
            String filename = getFilenameFromUri(context, uri);
            File cacheFile = new File(context.getCacheDir(), filename);

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(cacheFile)) {

                if (inputStream == null) return null;

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return cacheFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getFilenameFromUri(Context context, Uri uri) {
        String filename = "upload_" + System.currentTimeMillis();
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) filename = cursor.getString(nameIndex);
            }
        }
        return filename;
    }
}