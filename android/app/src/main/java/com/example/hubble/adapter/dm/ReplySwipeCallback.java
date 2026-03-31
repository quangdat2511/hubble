package com.example.hubble.adapter.dm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;

public class ReplySwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnReplyListener {
        void onReply(int position);
    }

    private static final float MAX_SWIPE_DP = 88f;
    private static final float TRIGGER_FRACTION = 0.58f;

    private final OnReplyListener listener;
    private final Drawable replyIcon;
    private final int iconSizePx;
    private final int iconMarginPx;
    private final int iconColor;
    private final float maxSwipePx;
    private final Paint circlePaint;
    private final int circleColor;

    private boolean hapticFired = false;

    public ReplySwipeCallback(Context context, OnReplyListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;

        float density = context.getResources().getDisplayMetrics().density;
        maxSwipePx = MAX_SWIPE_DP * density;
        iconSizePx = (int) (24 * density);
        iconMarginPx = (int) (16 * density);

        iconColor = resolveAttrColor(context, com.google.android.material.R.attr.colorOnSurface);
        circleColor = resolveAttrColor(context, com.google.android.material.R.attr.colorSurfaceVariant);

        replyIcon = ContextCompat.getDrawable(context, R.drawable.ic_reply);
        if (replyIcon != null) {
            replyIcon.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
        }

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(circleColor);
    }

    private static int resolveAttrColor(Context context, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv,
                          @NonNull RecyclerView.ViewHolder vh,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) {
            listener.onReply(pos);
        }
        // Reset the item — notifyItemChanged will snap it back
        if (pos != RecyclerView.NO_POSITION && viewHolder.getBindingAdapter() != null) {
            viewHolder.getBindingAdapter().notifyItemChanged(pos);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Trigger when drag exceeds TRIGGER_FRACTION * maxSwipePx relative to item width.
        int itemWidth = viewHolder.itemView.getWidth();
        if (itemWidth == 0) return 0.15f;
        return (maxSwipePx * TRIGGER_FRACTION) / itemWidth;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 4f;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 3f;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        // Ignore opposite direction and cap visual swipe distance.
        if (dX > 0f) {
            dX = 0f;
        }

        // Cap the visual swipe at maxSwipePx
        float clampedDx = Math.max(dX, -maxSwipePx);
        float swipeProgress = Math.abs(clampedDx) / maxSwipePx; // 0.0 → 1.0

        // Fire haptic once when threshold is crossed
        if (swipeProgress >= TRIGGER_FRACTION && !hapticFired) {
            hapticFired = true;
            viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        } else if (swipeProgress < TRIGGER_FRACTION) {
            hapticFired = false;
        }

        // Draw reply icon on item right edge.
        if (replyIcon != null) {
            android.view.View itemView = viewHolder.itemView;
            int itemCenterY = itemView.getTop() + itemView.getHeight() / 2;

            int circleRadius = (int) (iconSizePx * 0.85f);
            int circleCenterX = itemView.getRight() - iconMarginPx - circleRadius;

            float alpha = Math.min(swipeProgress / 0.3f, 1f);
            float scale = 0.6f + 0.4f * Math.min(swipeProgress / 0.5f, 1f);

            // Draw circle background
            circlePaint.setAlpha((int) (alpha * 200));
            c.save();
            c.scale(scale, scale, circleCenterX, itemCenterY);
            c.drawCircle(circleCenterX, itemCenterY, circleRadius, circlePaint);
            c.restore();

            // Draw reply icon centered in circle
            int half = iconSizePx / 2;
            replyIcon.setBounds(
                    circleCenterX - half,
                    itemCenterY - half,
                    circleCenterX + half,
                    itemCenterY + half
            );
            replyIcon.setAlpha((int) (alpha * 255));
            c.save();
            c.scale(scale, scale, circleCenterX, itemCenterY);
            replyIcon.draw(c);
            c.restore();
        }

        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setTranslationX(0f);
        hapticFired = false;
    }
}
