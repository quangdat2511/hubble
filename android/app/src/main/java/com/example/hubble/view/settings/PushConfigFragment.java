package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.databinding.FragmentPushConfigBinding;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;

public class PushConfigFragment extends Fragment {

    private FragmentPushConfigBinding binding;
    private SettingsViewModel viewModel;
    private boolean isApplyingPushConfig;
    private boolean isPushConfigBusy;
    private PushConfigResponse lastKnownPushConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPushConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        setupPushConfig();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireSettingsActivity().updateTitle(R.string.settings_notifications);
    }

    private void setupPushConfig() {
        binding.rowPushEnabled.setOnClickListener(v -> {
            if (!isPushConfigBusy) {
                binding.switchPushEnabled.toggle();
            }
        });

        binding.rowPushSound.setOnClickListener(v -> {
            if (!isPushConfigBusy && binding.switchPushSound.isEnabled()) {
                binding.switchPushSound.toggle();
            }
        });

        binding.switchPushEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isApplyingPushConfig) {
                return;
            }

            updatePushSoundAvailability(isChecked);
            viewModel.updatePushConfig(isChecked, binding.switchPushSound.isChecked());
        });

        binding.switchPushSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isApplyingPushConfig) {
                return;
            }

            viewModel.updatePushConfig(binding.switchPushEnabled.isChecked(), isChecked);
        });

        viewModel.pushConfigState.observe(getViewLifecycleOwner(), this::renderPushConfigLoadState);
        viewModel.pushConfigSaveState.observe(getViewLifecycleOwner(), this::renderPushConfigSaveState);
        viewModel.loadPushConfig();
    }

    private void renderPushConfigLoadState(AuthResult<PushConfigResponse> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            setPushConfigBusy(true);
            return;
        }

        setPushConfigBusy(false);
        viewModel.resetPushConfigState();

        if (result.isSuccess() && result.getData() != null) {
            lastKnownPushConfig = result.getData();
            applyPushConfig(result.getData());
            return;
        }

        Snackbar.make(binding.getRoot(),
                result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }

    private void renderPushConfigSaveState(AuthResult<PushConfigResponse> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            setPushConfigBusy(true);
            return;
        }

        setPushConfigBusy(false);
        viewModel.resetPushConfigSaveState();

        if (result.isSuccess() && result.getData() != null) {
            lastKnownPushConfig = result.getData();
            applyPushConfig(result.getData());
            Snackbar.make(binding.getRoot(),
                    getString(R.string.settings_notifications_saved),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (lastKnownPushConfig != null) {
            applyPushConfig(lastKnownPushConfig);
        }
        Snackbar.make(binding.getRoot(),
                result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }

    private void applyPushConfig(PushConfigResponse config) {
        isApplyingPushConfig = true;
        binding.switchPushEnabled.setChecked(config.isNotificationEnabled());
        binding.switchPushSound.setChecked(config.isNotificationSound());
        updatePushSoundAvailability(config.isNotificationEnabled());
        isApplyingPushConfig = false;
    }

    private void updatePushSoundAvailability(boolean notificationEnabled) {
        boolean soundEnabled = notificationEnabled && !isPushConfigBusy;
        binding.switchPushSound.setEnabled(soundEnabled);
        binding.rowPushSound.setEnabled(soundEnabled);
        binding.rowPushSound.setAlpha(notificationEnabled ? 1f : 0.5f);
        binding.textPushSoundSummary.setText(notificationEnabled
                ? R.string.settings_notifications_sound_summary
                : R.string.settings_notifications_sound_disabled_summary);
    }

    private void setPushConfigBusy(boolean isBusy) {
        isPushConfigBusy = isBusy;
        requireSettingsActivity().setScreenLoading(isBusy);
        binding.rowPushEnabled.setEnabled(!isBusy);
        binding.switchPushEnabled.setEnabled(!isBusy);
        updatePushSoundAvailability(binding.switchPushEnabled.isChecked());
    }

    private SettingsActivity requireSettingsActivity() {
        return (SettingsActivity) requireActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
