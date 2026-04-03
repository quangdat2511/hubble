package com.example.hubble.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.hubble.R;

import java.util.Locale;

public final class AvatarPlaceholderUtils {

    private static final String FALLBACK_LETTER = "?";
    private static final int DEFAULT_SIZE_DP = 40;

    private AvatarPlaceholderUtils() {
    }

    @NonNull
    public static Drawable createAvatarDrawable(
            @NonNull Context context,
            @Nullable String displayName,
            int requestedSizePx
    ) {
        int sizePx = resolveSizePx(context, requestedSizePx);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(resolveBackgroundColor(context));
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint);

        String letter = resolveLetter(displayName);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sizePx * 0.46f);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textBaseline = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(letter, sizePx / 2f, textBaseline, textPaint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @NonNull
    public static Drawable createDefaultAvatarDrawable(@NonNull Context context, int requestedSizePx) {
        int sizePx = resolveSizePx(context, requestedSizePx);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(resolveBackgroundColor(context));
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint);

        Drawable icon = AppCompatResources.getDrawable(context, R.drawable.ic_person);
        if (icon == null) {
            return new BitmapDrawable(context.getResources(), bitmap);
        }

        Drawable wrappedIcon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(wrappedIcon, Color.WHITE);
        int iconInset = Math.round(sizePx * 0.24f);
        wrappedIcon.setBounds(iconInset, iconInset, sizePx - iconInset, sizePx - iconInset);
        wrappedIcon.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @NonNull
    private static String resolveLetter(@Nullable String displayName) {
        if (displayName == null) {
            return FALLBACK_LETTER;
        }

        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return FALLBACK_LETTER;
        }

        int firstCodePoint = trimmed.codePointAt(0);
        return new String(Character.toChars(firstCodePoint)).toUpperCase(Locale.getDefault());
    }

    private static int resolveBackgroundColor(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.color_success);
    }

    private static int resolveSizePx(@NonNull Context context, int requestedSizePx) {
        return requestedSizePx > 0
                ? requestedSizePx
                : Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SIZE_DP,
                context.getResources().getDisplayMetrics()
        ));
    }
}
