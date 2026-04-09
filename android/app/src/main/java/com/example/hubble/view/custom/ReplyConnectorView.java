package com.example.hubble.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/**
 * Discord-style reply connector (└─ shape): vertical line at the centre of
 * the 40 dp avatar column, smooth curve, then horizontal line to the right.
 */
public class ReplyConnectorView extends View {

    private static final float AVATAR_COLUMN_DP = 40f;
    private static final float CORNER_RADIUS_DP = 7f;
    private static final float STROKE_DP = 2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public ReplyConnectorView(Context context) {
        super(context);
        init(context);
    }

    public ReplyConnectorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReplyConnectorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int color = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutline, Color.GRAY);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath(w, h);
    }

    private void buildPath(int w, int h) {
        if (w <= 0 || h <= 0) return;

        float d = getResources().getDisplayMetrics().density;
        paint.setStrokeWidth(STROKE_DP * d);

        float cx = (AVATAR_COLUMN_DP / 2f) * d;
        float cy = h / 2f;
        // Tính bán kính bo góc (đảm bảo không vượt quá kích thước view)
        float r = Math.min(CORNER_RADIUS_DP * d, Math.min(w - cx, cy));

        path.reset();

        // Tạo hình ┌─ (Bắt đầu từ dưới đáy view, đi thẳng lên, uốn cong sang phải)

        // 1. Đường thẳng đứng: Bắt đầu từ CẠNH DƯỚI của view (nối với avatar của tin nhắn hiện tại)
        path.moveTo(cx, h);

        // Kéo đường thẳng lên trên, dừng lại cách tâm 'cy' một khoảng 'r' để chuẩn bị bo góc
        path.lineTo(cx, cy + r);

        // 2. Góc bo tròn: Uốn cong mượt sang phải bằng quadTo
        // (cx, cy) là điểm neo (control point) vuông góc
        // (cx + r, cy) là điểm kết thúc đường cong
        path.quadTo(cx, cy, cx + r, cy);

        // 3. Đường ngang: Kéo thẳng sang mép phải của view (chỉ vào tin nhắn được reply)
        path.lineTo(w, cy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}
