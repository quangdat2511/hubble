package com.example.hubble.view.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.ThemeManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

public class ThemeFragment extends Fragment {

    private static final String TAG = "ThemeFragment";

    private MaterialCardView lightOption;
    private MaterialCardView darkOption;
    private ImageView lightSelectedIcon;
    private ImageView darkSelectedIcon;
    private SettingsViewModel settingsViewModel;
    private String authHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_theme, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lightOption = view.findViewById(R.id.optionLight);
        darkOption = view.findViewById(R.id.optionDark);
        lightSelectedIcon = view.findViewById(R.id.iconLightSelected);
        darkSelectedIcon = view.findViewById(R.id.iconDarkSelected);
        settingsViewModel = new ViewModelProvider(
                requireActivity(),
                new SettingsViewModelFactory(
                        new AuthRepository(requireContext()),
                        new SettingsRepository(requireContext()),
                        new PushConfigRepository(requireContext()))
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();
        authHeader = TextUtils.isEmpty(token) ? null : "Bearer " + token;

        String initialTheme = ThemeManager.getSavedTheme(requireContext());
        settingsViewModel.setCachedTheme(initialTheme);
        applyThemeSelection(initialTheme);
        setOptionsEnabled(!settingsViewModel.isThemeUpdateInProgress());

        observeThemeState();
        observeThemeUpdateState();
        setupOptionListeners();
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
                setOptionsEnabled(false);
                return;
            }

            setOptionsEnabled(true);

            if (result.isSuccess()) {
                String updatedTheme = ThemeManager.normalizeTheme(result.getData());
                Log.d(TAG, "updateTheme() succeeded with theme = " + updatedTheme);
                settingsViewModel.setCachedTheme(updatedTheme);
                applyThemeSelection(updatedTheme);
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

    private void setupOptionListeners() {
        lightOption.setOnClickListener(v -> handleThemeSelection(ThemeManager.THEME_LIGHT));
        darkOption.setOnClickListener(v -> handleThemeSelection(ThemeManager.THEME_DARK));
    }

    private void handleThemeSelection(String newTheme) {
        String previousTheme = settingsViewModel.getCachedTheme();
        if (newTheme.equals(previousTheme)) {
            return;
        }

        Log.d(TAG, "User selected theme " + newTheme);
        settingsViewModel.markLocalOverride();

        if (TextUtils.isEmpty(authHeader)) {
            applyThemeLocally(newTheme);
            return;
        }

        setOptionsEnabled(false);
        settingsViewModel.updateTheme(authHeader, newTheme, previousTheme);
        applyThemeLocally(newTheme);
    }

    private void applyThemeLocally(String theme) {
        String normalizedTheme = ThemeManager.normalizeTheme(theme);
        settingsViewModel.setCachedTheme(normalizedTheme);
        ThemeManager.saveTheme(requireContext(), normalizedTheme);
        applyThemeSelection(normalizedTheme);
    }

    private void applyThemeSelection(String theme) {
        boolean isLight = ThemeManager.THEME_LIGHT.equals(ThemeManager.normalizeTheme(theme));
        updateOptionAppearance(lightOption, lightSelectedIcon, isLight);
        updateOptionAppearance(darkOption, darkSelectedIcon, !isLight);
    }

    private void updateOptionAppearance(MaterialCardView option, ImageView selectedIcon, boolean isSelected) {
        int strokeColor = ContextCompat.getColor(
                requireContext(),
                isSelected ? R.color.color_primary : R.color.color_divider
        );
        option.setStrokeColor(strokeColor);
        option.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
        selectedIcon.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
    }

    private void setOptionsEnabled(boolean isEnabled) {
        setOptionEnabled(lightOption, isEnabled);
        setOptionEnabled(darkOption, isEnabled);
    }

    private void setOptionEnabled(MaterialCardView option, boolean isEnabled) {
        option.setEnabled(isEnabled);
        option.setClickable(isEnabled);
        option.setAlpha(isEnabled ? 1f : 0.6f);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showPendingThemeErrorIfNeeded(View view) {
        String errorMessage = settingsViewModel.consumePendingThemeErrorMessage();
        if (!TextUtils.isEmpty(errorMessage)) {
            Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }
}
