package com.example.hubble.view.server;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.databinding.DialogColorPickerBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Bottom sheet that shows a grid of Discord role colors for selection.
 * Call {@link #newInstance(int)} passing the current color, then
 * {@link #setOnColorSelectedListener} to receive the picked color.
 */
public class ColorPickerBottomSheet extends BottomSheetDialogFragment {

    // Row-by-row order matching the Discord color picker screenshot:
    // Row 1: bright tones  Row 2: dark tones  Row 3: warm+neutral  Row 4: earth tones
    private static final int[] DISCORD_COLORS = {
            0xFF1ABC9C, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFFE91E63,
            0xFF11806A, 0xFF1F8B4C, 0xFF206694, 0xFF71368A, 0xFFAD1457,
            0xFFF1C40F, 0xFFE67E22, 0xFFE74C3C, 0xFF95A5A6, 0xFF607D8B,
            0xFFC27C0E, 0xFFA84300, 0xFF992D22,
    };

    private static final int DEFAULT_COLOR = 0xFF99AAB5;
    private static final int EYEDROPPER_BG  = 0xFF383A40;
    private static final int EYEDROPPER_TINT = 0xFFB5BAC1;
    private static final int ITEMS_PER_ROW  = 5;
    private static final int NUM_ROWS       = 4; // ceil(18 colors + 1 eyedropper, 5 per row)

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private DialogColorPickerBinding binding;
    private int pendingColor;
    private int customColor = -1;  // -1 means no custom color has been entered
    private OnColorSelectedListener listener;
    private final View[] swatchViews = new View[DISCORD_COLORS.length];
    private FrameLayout eyedropperCell;
    private ImageView eyedropperIcon;

    public static ColorPickerBottomSheet newInstance(int initialColor) {
        ColorPickerBottomSheet sheet = new ColorPickerBottomSheet();
        Bundle args = new Bundle();
        args.putInt("initial_color", initialColor);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogColorPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pendingColor = getArguments() != null
                ? getArguments().getInt("initial_color", DEFAULT_COLOR)
                : DEFAULT_COLOR;

        buildColorGrid();

        binding.btnSave.setOnClickListener(v -> {
            if (listener != null) listener.onColorSelected(pendingColor);
            dismiss();
        });

        binding.btnReset.setOnClickListener(v -> {
            pendingColor = DEFAULT_COLOR;
            customColor = -1;
            refreshSwatches();
        });
    }

    // ─── Grid builder ────────────────────────────────────────────────────────

    private void buildColorGrid() {
        Context ctx = requireContext();
        int swatchPx  = dpToPx(52);
        int marginPx  = dpToPx(4);

        for (int row = 0; row < NUM_ROWS; row++) {
            LinearLayout rowLayout = new LinearLayout(ctx);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int index = row * ITEMS_PER_ROW + col;

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, swatchPx, 1f);
                params.setMargins(marginPx, marginPx, marginPx, marginPx);

                if (index < DISCORD_COLORS.length) {
                    rowLayout.addView(createColorSwatch(ctx, index, params));
                } else if (index == DISCORD_COLORS.length) {
                    rowLayout.addView(createEyedropperSwatch(ctx, params));
                } else {
                    // Empty spacer to keep grid alignment on last row
                    View spacer = new View(ctx);
                    spacer.setLayoutParams(params);
                    rowLayout.addView(spacer);
                }
            }

            binding.colorSwatchContainer.addView(rowLayout);
        }
    }

    /** Circular color swatch with selection ring when active. */
    private View createColorSwatch(Context ctx, int index, LinearLayout.LayoutParams params) {
        final int color = DISCORD_COLORS[index];
        View swatch = new View(ctx);
        swatch.setLayoutParams(params);
        swatch.setBackground(buildSwatchDrawable(color, color == pendingColor));
        swatch.setOnClickListener(v -> {
            pendingColor = color;
            refreshSwatches();
        });
        swatchViews[index] = swatch;
        return swatch;
    }

    /** Eyedropper slot — dark circle + pipette icon, opens hex color input on tap. */
    private View createEyedropperSwatch(Context ctx, LinearLayout.LayoutParams params) {
        eyedropperCell = new FrameLayout(ctx);
        eyedropperCell.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(EYEDROPPER_BG);
        eyedropperCell.setBackground(bg);

        eyedropperIcon = new ImageView(ctx);
        int iconPx = dpToPx(22);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconPx, iconPx);
        iconParams.gravity = Gravity.CENTER;
        eyedropperIcon.setLayoutParams(iconParams);
        eyedropperIcon.setImageResource(R.drawable.ic_eyedropper);
        eyedropperIcon.setColorFilter(EYEDROPPER_TINT);
        eyedropperCell.addView(eyedropperIcon);

        eyedropperCell.setOnClickListener(v -> showColorSpectrumDialog());

        return eyedropperCell;
    }

    // ─── Swatch state ────────────────────────────────────────────────────────

    /** Rebuild all swatch backgrounds (preset + eyedropper) to reflect the current pendingColor. */
    private void refreshSwatches() {
        for (int i = 0; i < swatchViews.length; i++) {
            if (swatchViews[i] != null) {
                swatchViews[i].setBackground(
                        buildSwatchDrawable(DISCORD_COLORS[i], DISCORD_COLORS[i] == pendingColor));
            }
        }
        refreshEyedropperCell();
    }

    /** Update the eyedropper cell to show the custom color (if selected) or the default dark icon state. */
    private void refreshEyedropperCell() {
        if (eyedropperCell == null) return;
        boolean customSelected = (customColor != -1 && pendingColor == customColor);
        if (customSelected) {
            eyedropperCell.setBackground(buildSwatchDrawable(customColor, true));
            if (eyedropperIcon != null) eyedropperIcon.setVisibility(View.GONE);
        } else {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(EYEDROPPER_BG);
            eyedropperCell.setBackground(bg);
            if (eyedropperIcon != null) eyedropperIcon.setVisibility(View.VISIBLE);
        }
    }

    /** Opens a dialog with a draggable hue spectrum bar — drag the needle to pick any color. */
    private void showColorSpectrumDialog() {
        Context ctx = requireContext();
        int pad = dpToPx(20);
        int gap = dpToPx(16);

        // Determine starting hue from current custom color, default to red
        float[] initHsv = new float[3];
        int initColor = (customColor != -1 && pendingColor == customColor) ? customColor : 0xFFFF0000;
        Color.colorToHSV(initColor, initHsv);

        // Color preview circle
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setShape(GradientDrawable.OVAL);
        previewBg.setColor(initColor);
        View preview = new View(ctx);
        int previewSize = dpToPx(56);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(previewSize, previewSize);
        previewParams.gravity = Gravity.CENTER_HORIZONTAL;
        previewParams.bottomMargin = gap;
        preview.setLayoutParams(previewParams);
        preview.setBackground(previewBg);

        // Hue spectrum bar
        ColorSpectrumView spectrum = new ColorSpectrumView(ctx);
        LinearLayout.LayoutParams specParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40));
        spectrum.setLayoutParams(specParams);
        spectrum.setListener(hue -> previewBg.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f})));
        spectrum.post(() -> spectrum.setHueFraction(initHsv[0] / 360f));

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad / 2);
        container.addView(preview);
        container.addView(spectrum);

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.color_picker_custom_title)
                .setView(container)
                .setPositiveButton(R.string.color_picker_save, (dialog, which) -> {
                    int picked = spectrum.getColor();
                    customColor = picked;
                    pendingColor = picked;
                    refreshSwatches();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Creates an oval Drawable for a color swatch.
     * When selected, adds a 3dp white stroke inside the circle boundary.
     */
    private GradientDrawable buildSwatchDrawable(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        if (selected) {
            d.setStroke(dpToPx(3), Color.WHITE);
        }
        return d;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─── Spectrum picker view ─────────────────────────────────────────────────

    /** Horizontal hue-spectrum bar with a draggable circular thumb/needle. */
    private static class ColorSpectrumView extends View {

        interface OnHueChangedListener { void onHueChanged(float hue); }

        private static final int[] SPECTRUM = {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00,
            0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
        };

        private final Paint barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float thumbFraction = 0f;
        private OnHueChangedListener listener;

        public ColorSpectrumView(Context ctx) {
            super(ctx);
            setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for shadow
            thumbFill.setColor(Color.WHITE);
            thumbFill.setShadowLayer(6f, 0f, 2f, 0x66000000);
            thumbRing.setStyle(Paint.Style.STROKE);
            thumbRing.setColor(0x33000000);
            thumbRing.setStrokeWidth(ctx.getResources().getDisplayMetrics().density * 2f);
        }

        void setListener(OnHueChangedListener l) { this.listener = l; }

        /** Position the thumb at the given fraction (0.0–1.0) of the bar width. */
        void setHueFraction(float f) {
            thumbFraction = Math.max(0f, Math.min(1f, f));
            invalidate();
        }

        float getHue()   { return thumbFraction * 360f; }
        int   getColor() { return Color.HSVToColor(new float[]{getHue(), 1f, 1f}); }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            barPaint.setShader(
                new LinearGradient(0, 0, w, 0, SPECTRUM, null, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), r = h / 2f;
            // Hue bar (rounded pill)
            canvas.drawRoundRect(0, 0, w, h, r, r, barPaint);
            // Thumb — clamp so it never clips outside the bar ends
            float tx = Math.max(r, Math.min(thumbFraction * w, w - r));
            canvas.drawCircle(tx, h / 2f, r * 1.25f, thumbFill);
            canvas.drawCircle(tx, h / 2f, r * 1.25f, thumbRing);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                thumbFraction = Math.max(0f, Math.min(e.getX() / getWidth(), 1f));
                invalidate();
                if (listener != null) listener.onHueChanged(getHue());
                return true;
            }
            return super.onTouchEvent(e);
        }
    }
}
