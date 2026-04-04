package com.example.hubble.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AudioWaveformView extends View {
    private Paint paint;
    private List<Float> amplitudes = new ArrayList<>();
    private float maxAmplitude = 100f; // Ngưỡng mặc định để chuẩn hóa
    private float barWidth = 8f; // Độ rộng của mỗi cột sóng
    private float space = 6f;    // Khoảng cách giữa các cột

    public AudioWaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        // Đổi màu này thành màu bạn thích (Ví dụ: màu Primary của app)
        paint.setColor(0xFF007AFF); // Màu xanh Blue mặc định
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth);
        paint.setAntiAlias(true);
    }

    // Nhận âm lượng truyền vào và yêu cầu vẽ lại màn hình
    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        if (amplitude > maxAmplitude) maxAmplitude = amplitude;

        // Nếu sóng vẽ tràn màn hình thì xóa bớt cái cũ ở đầu đi
        int maxBars = (int) (getWidth() / (barWidth + space));
        if (amplitudes.size() > maxBars) {
            amplitudes.remove(0);
        }
        invalidate(); // Gọi onDraw
    }

    // Xóa sóng âm (khi dừng ghi âm)
    public void clear() {
        amplitudes.clear();
        maxAmplitude = 100f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (amplitudes.isEmpty()) return;

        float centerY = getHeight() / 2f;
        float startX = getWidth() - (amplitudes.size() * (barWidth + space));
        if (startX < 0) startX = 0;

        for (float amp : amplitudes) {
            // Tính chiều cao cột sóng (tối đa bằng chiều cao của View)
            float barHeight = (amp / maxAmplitude) * (getHeight() * 0.8f);
            if (barHeight < 4f) barHeight = 4f; // Chiều cao tối thiểu khi im lặng

            float top = centerY - (barHeight / 2f);
            float bottom = centerY + (barHeight / 2f);

            canvas.drawLine(startX, top, startX, bottom, paint);
            startX += barWidth + space;
        }
    }
}