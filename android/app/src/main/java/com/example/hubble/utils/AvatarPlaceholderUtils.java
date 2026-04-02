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

import java.util.Locale;
import java.util.Random;

public final class AvatarPlaceholderUtils {

    private static final String FALLBACK_LETTER = "?";
    private static final float MIN_SATURATION = 0.55f;
    private static final float MAX_SATURATION = 0.85f;
    private static final float MIN_VALUE = 0.55f;
    private static final float MAX_VALUE = 0.80f;
    private static final int DEFAULT_SIZE_DP = 40;

    private AvatarPlaceholderUtils() {
    }

    @NonNull
    public static Drawable createAvatarDrawable(
            @NonNull Context context,
            @Nullable String displayName,
            int requestedSizePx
    ) {
        int sizePx = requestedSizePx > 0
                ? requestedSizePx
                : Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SIZE_DP,
                context.getResources().getDisplayMetrics()
        ));

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(resolveBackgroundColor(displayName));
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

    private static int resolveBackgroundColor(@Nullable String seedSource) {
        String seed = seedSource == null ? FALLBACK_LETTER : seedSource.trim();
        if (seed.isEmpty()) {
            seed = FALLBACK_LETTER;
        }

        Random random = new Random(seed.hashCode());
        float hue = random.nextInt(360);
        float saturation = MIN_SATURATION + (MAX_SATURATION - MIN_SATURATION) * random.nextFloat();
        float value = MIN_VALUE + (MAX_VALUE - MIN_VALUE) * random.nextFloat();
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }
}
