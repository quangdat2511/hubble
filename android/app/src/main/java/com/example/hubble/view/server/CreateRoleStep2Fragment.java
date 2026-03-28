package com.example.hubble.view.server;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.adapter.server.PermissionPresetAdapter;
import com.example.hubble.databinding.FragmentCreateRoleStep2Binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Create Role Step 2 (#8,9,10,11) - Permission preset selection via slider + swipeable cards.
 */
public class CreateRoleStep2Fragment extends Fragment {

    private FragmentCreateRoleStep2Binding binding;
    private String serverId;
    private String roleName;
    private int roleColor;

    private final int[] presetColors = new int[4];

    public static CreateRoleStep2Fragment newInstance(String serverId, String roleName, int roleColor) {
        CreateRoleStep2Fragment fragment = new CreateRoleStep2Fragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("role_name", roleName);
        args.putInt("role_color", roleColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateRoleStep2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            roleName = getArguments().getString("role_name");
            roleColor = getArguments().getInt("role_color");
        }

        binding.toolbar.setTitle(getString(R.string.create_role_step, 2));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        presetColors[0] = ContextCompat.getColor(requireContext(), R.color.role_preset_decorative);
        presetColors[1] = ContextCompat.getColor(requireContext(), R.color.role_preset_member);
        presetColors[2] = ContextCompat.getColor(requireContext(), R.color.role_preset_moderator);
        presetColors[3] = ContextCompat.getColor(requireContext(), R.color.role_preset_admin);

        setupPresetCards();
        setupSlider();
        setupButtons();
    }

    private void setupPresetCards() {
        List<PermissionPresetAdapter.PresetData> presets = new ArrayList<>();

        presets.add(new PermissionPresetAdapter.PresetData(
                getString(R.string.create_role_perm_decorative),
                getString(R.string.create_role_preset_decorative_desc),
                null,
                Arrays.asList(
                        getString(R.string.create_role_preset_decorative_1),
                        getString(R.string.create_role_preset_decorative_2))));

        presets.add(new PermissionPresetAdapter.PresetData(
                getString(R.string.create_role_perm_member),
                getString(R.string.create_role_preset_member_desc),
                null,
                Arrays.asList(
                        getString(R.string.create_role_preset_member_1),
                        getString(R.string.create_role_preset_member_2),
                        getString(R.string.create_role_preset_member_3))));

        presets.add(new PermissionPresetAdapter.PresetData(
                getString(R.string.create_role_perm_moderator),
                getString(R.string.create_role_preset_moderator_desc),
                getString(R.string.create_role_preset_moderator_extra),
                Arrays.asList(
                        getString(R.string.create_role_preset_moderator_1),
                        getString(R.string.create_role_preset_moderator_2),
                        getString(R.string.create_role_preset_moderator_3),
                        getString(R.string.create_role_preset_moderator_4))));

        presets.add(new PermissionPresetAdapter.PresetData(
                getString(R.string.create_role_perm_admin),
                getString(R.string.create_role_preset_admin_desc),
                getString(R.string.create_role_preset_admin_extra),
                Arrays.asList(
                        getString(R.string.create_role_preset_admin_1),
                        getString(R.string.create_role_preset_admin_2),
                        getString(R.string.create_role_preset_admin_3),
                        getString(R.string.create_role_preset_admin_4))));

        PermissionPresetAdapter adapter = new PermissionPresetAdapter(presets);
        binding.vpPresets.setAdapter(adapter);
        binding.vpPresets.setOffscreenPageLimit(1);

        // Sync ViewPager with slider
        binding.vpPresets.registerOnPageChangeCallback(
                new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        binding.sliderPreset.setValue(position);
                        updateSliderColor(position);
                    }
                });
    }

    private void setupSlider() {
        binding.sliderPreset.setValue(0);
        updateSliderColor(0);

        binding.sliderPreset.addOnChangeListener((slider, value, fromUser) -> {
            int position = (int) value;
            if (fromUser) {
                binding.vpPresets.setCurrentItem(position, true);
            }
            updateSliderColor(position);
        });
    }

    private void updateSliderColor(int position) {
        int color = presetColors[position];
        binding.sliderPreset.setThumbTintList(ColorStateList.valueOf(color));
        binding.sliderPreset.setTrackActiveTintList(ColorStateList.valueOf(color));
    }

    private static final String[] PRESET_NAMES = {"DECORATIVE", "MEMBER", "MODERATOR", "ADMIN"};

    private void setupButtons() {
        View.OnClickListener goToStep3 = v -> {
            int position = (int) binding.sliderPreset.getValue();
            String preset = PRESET_NAMES[position];
            ((ServerSettingsActivity) requireActivity()).navigateTo(
                    CreateRoleStep3Fragment.newInstance(serverId, roleName, roleColor, preset), true);
        };

        binding.btnChoose.setOnClickListener(goToStep3);
        binding.btnSkip.setOnClickListener(goToStep3);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
