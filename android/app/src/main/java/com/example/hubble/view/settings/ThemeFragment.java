package com.example.hubble.view.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class ThemeFragment extends Fragment {

    private static final String TAG = "ThemeFragment";

    private Switch switchTheme;
    private SettingsViewModel settingsViewModel;
    private String authHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_theme, container, false);

        switchTheme = view.findViewById(R.id.switchTheme);

        settingsViewModel = new ViewModelProvider(
                this,
                new SettingsViewModelFactory(new SettingsRepository(requireContext()))
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();

        Log.d(TAG, "Raw token = " + token);

        if (!TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
            Log.d(TAG, "Auth header created");

            settingsViewModel.getTheme(authHeader).observe(getViewLifecycleOwner(), theme -> {
                Log.d(TAG, "getTheme() returned theme = " + theme);
                Toast.makeText(requireContext(), "Loaded theme: " + theme, Toast.LENGTH_SHORT).show();

                boolean isDark = "dark".equalsIgnoreCase(theme);
                Log.d(TAG, "Switch setChecked = " + isDark);

                switchTheme.setOnCheckedChangeListener(null);
                switchTheme.setChecked(isDark);
                applyTheme(isDark);

                switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String newTheme = isChecked ? "dark" : "light";

                    Log.d(TAG, "User toggled switch. isChecked = " + isChecked);
                    Log.d(TAG, "Calling updateTheme() with value = " + newTheme);
                    Toast.makeText(requireContext(), "Updating theme to: " + newTheme, Toast.LENGTH_SHORT).show();

                    applyTheme(isChecked);
                    settingsViewModel.updateTheme(authHeader, newTheme);
                });
            });
        } else {
            Log.e(TAG, "Token is null or empty");

            Toast.makeText(requireContext(), "No token found", Toast.LENGTH_SHORT).show();

            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "No token mode. Local theme only. isChecked = " + isChecked);
                applyTheme(isChecked);
            });
        }

        return view;
    }

    private void applyTheme(boolean isDark) {
        Log.d(TAG, "applyTheme() called. isDark = " + isDark);

        AppCompatDelegate.setDefaultNightMode(
                isDark
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}