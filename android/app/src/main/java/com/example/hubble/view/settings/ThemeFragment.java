package com.example.hubble.view.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.ThemeManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class ThemeFragment extends Fragment {

    private static final String TAG = "ThemeFragment";

    private Switch switchTheme;
    private SettingsViewModel settingsViewModel;
    private String authHeader;
    private boolean isProgrammaticSwitchChange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_theme, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchTheme = view.findViewById(R.id.switchTheme);
        settingsViewModel = new ViewModelProvider(
                requireActivity(),
                new SettingsViewModelFactory(new SettingsRepository(requireContext()))
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();
        authHeader = TextUtils.isEmpty(token) ? null : "Bearer " + token;

        String initialTheme = ThemeManager.getSavedTheme(requireContext());
        settingsViewModel.setCachedTheme(initialTheme);
        applySwitchTheme(initialTheme);
        setSwitchEnabled(!settingsViewModel.isThemeUpdateInProgress());

        observeThemeState();
        observeThemeUpdateState();
        setupSwitchListener();
        showPendingThemeErrorIfNeeded(view);

        Log.d(TAG, "Raw token = " + token);
        if (!TextUtils.isEmpty(authHeader) && !settingsViewModel.isThemeUpdateInProgress()) {
            Log.d(TAG, "Fetching theme from server");
            settingsViewModel.getTheme(authHeader);
        } else {
            Log.d(TAG, "No authenticated theme fetch needed");
        }
    }

    private void observeThemeState() {
        settingsViewModel.getThemeState().observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) {
                return;
            }

            if (result.isSuccess()) {
                String remoteTheme = ThemeManager.normalizeTheme(result.getData());
                Log.d(TAG, "getTheme() returned theme = " + remoteTheme);

                if (!settingsViewModel.hasLocalOverrideSinceFetch()
                        && !settingsViewModel.isThemeUpdateInProgress()) {
                    applyThemeLocally(remoteTheme);
                }
            } else {
                Log.w(TAG, "getTheme() failed, keeping local theme: " + result.getMessage());
            }

            settingsViewModel.clearThemeState();
        });
    }

    private void observeThemeUpdateState() {
        settingsViewModel.getThemeUpdateState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }

            if (result.isLoading()) {
                setSwitchEnabled(false);
                return;
            }

            setSwitchEnabled(true);

            if (result.isSuccess()) {
                String updatedTheme = ThemeManager.normalizeTheme(result.getData());
                Log.d(TAG, "updateTheme() succeeded with theme = " + updatedTheme);
                settingsViewModel.setCachedTheme(updatedTheme);
                applySwitchTheme(updatedTheme);
                settingsViewModel.clearPendingPreviousTheme();
            } else {
                String fallbackTheme = settingsViewModel.getPendingPreviousTheme();
                if (TextUtils.isEmpty(fallbackTheme)) {
                    fallbackTheme = ThemeManager.getSavedTheme(requireContext());
                }

                Log.e(TAG, "updateTheme() failed, reverting to " + fallbackTheme);
                settingsViewModel.setPendingThemeErrorMessage(result.getMessage());
                settingsViewModel.clearPendingPreviousTheme();
                applyThemeLocally(fallbackTheme);
            }

            settingsViewModel.clearThemeUpdateState();
        });
    }

    private void setupSwitchListener() {
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticSwitchChange) {
                return;
            }

            String previousTheme = settingsViewModel.getCachedTheme();
            String newTheme = isChecked ? ThemeManager.THEME_DARK : ThemeManager.THEME_LIGHT;
            if (newTheme.equals(previousTheme)) {
                return;
            }

            Log.d(TAG, "User toggled theme to " + newTheme);
            settingsViewModel.markLocalOverride();

            if (TextUtils.isEmpty(authHeader)) {
                applyThemeLocally(newTheme);
                return;
            }

            setSwitchEnabled(false);
            settingsViewModel.updateTheme(authHeader, newTheme, previousTheme);
            applyThemeLocally(newTheme);
        });
    }

    private void applyThemeLocally(String theme) {
        String normalizedTheme = ThemeManager.normalizeTheme(theme);
        settingsViewModel.setCachedTheme(normalizedTheme);
        ThemeManager.saveTheme(requireContext(), normalizedTheme);
        applySwitchTheme(normalizedTheme);
    }

    private void applySwitchTheme(String theme) {
        boolean isDark = ThemeManager.THEME_DARK.equals(ThemeManager.normalizeTheme(theme));
        isProgrammaticSwitchChange = true;
        switchTheme.setChecked(isDark);
        isProgrammaticSwitchChange = false;
    }

    private void setSwitchEnabled(boolean isEnabled) {
        switchTheme.setEnabled(isEnabled);
        switchTheme.setAlpha(isEnabled ? 1f : 0.6f);
    }

    private void showPendingThemeErrorIfNeeded(View view) {
        String errorMessage = settingsViewModel.consumePendingThemeErrorMessage();
        if (!TextUtils.isEmpty(errorMessage)) {
            Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }
}
