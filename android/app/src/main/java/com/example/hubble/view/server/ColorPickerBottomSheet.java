package com.example.hubble.view.server;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import com.google.android.material.snackbar.Snackbar;

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
    private OnColorSelectedListener listener;
    private final View[] swatchViews = new View[DISCORD_COLORS.length];

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

    /** Eyedropper slot — dark circle + pipette icon, shows "coming soon". */
    private View createEyedropperSwatch(Context ctx, LinearLayout.LayoutParams params) {
        FrameLayout cell = new FrameLayout(ctx);
        cell.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(EYEDROPPER_BG);
        cell.setBackground(bg);

        ImageView icon = new ImageView(ctx);
        int iconPx = dpToPx(22);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconPx, iconPx);
        iconParams.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.ic_eyedropper);
        icon.setColorFilter(EYEDROPPER_TINT);
        cell.addView(icon);

        cell.setOnClickListener(v ->
                Snackbar.make(requireView(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());

        return cell;
    }

    // ─── Swatch state ────────────────────────────────────────────────────────

    /** Rebuild all swatch backgrounds to reflect the current pendingColor. */
    private void refreshSwatches() {
        for (int i = 0; i < swatchViews.length; i++) {
            if (swatchViews[i] != null) {
                swatchViews[i].setBackground(
                        buildSwatchDrawable(DISCORD_COLORS[i], DISCORD_COLORS[i] == pendingColor));
            }
        }
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
}
