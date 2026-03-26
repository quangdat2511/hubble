package com.example.hubble.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

public final class ImageSaveHelper {

    private ImageSaveHelper() {
    }

    public static Uri saveBitmapAsPng(Context context, Bitmap bitmap, String fileName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Hubble");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = context.getContentResolver().insert(collection, values);
        if (uri == null) {
            throw new IOException("Cannot create image entry");
        }

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IOException("Cannot write PNG file");
            }
        } catch (Exception e) {
            context.getContentResolver().delete(uri, null, null);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues completed = new ContentValues();
            completed.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(uri, completed, null, null);
        }

        return uri;
    }
}
