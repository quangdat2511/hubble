package com.example.hubble.view.server;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentCreateRoleStep1Binding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Create Role Step 1 (#7) - Name and color selection.
 */
public class CreateRoleStep1Fragment extends Fragment {

    private FragmentCreateRoleStep1Binding binding;
    private String serverId;
    private int selectedColor = Color.parseColor("#99AAB5");

    public static CreateRoleStep1Fragment newInstance(String serverId) {
        CreateRoleStep1Fragment fragment = new CreateRoleStep1Fragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateRoleStep1Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
        }

        binding.toolbar.setTitle(getString(R.string.create_role_step, 1));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        updateColorPreview();

        binding.rowColorPicker.setOnClickListener(v -> showColorPicker());

        binding.btnCreate.setOnClickListener(v -> {
            String name = binding.etRoleName.getText() != null
                    ? binding.etRoleName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Snackbar.make(view, R.string.error_empty_name, Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Proceed to step 2
            ((ServerSettingsActivity) requireActivity()).navigateTo(
                    CreateRoleStep2Fragment.newInstance(serverId, name, selectedColor), true);
        });
    }

    private void showColorPicker() {
        ColorPickerBottomSheet sheet = ColorPickerBottomSheet.newInstance(selectedColor);
        sheet.setOnColorSelectedListener(color -> {
            selectedColor = color;
            updateColorPreview();
        });
        sheet.show(getChildFragmentManager(), "color_picker");
    }

    private void updateColorPreview() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(selectedColor);
        binding.viewColorPreview.setBackground(circle);
        binding.tvColorHex.setText(String.format("#%06X", 0xFFFFFF & selectedColor));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
