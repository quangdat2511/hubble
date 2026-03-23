package com.example.hubble.view.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class LanguageFragment extends Fragment {

    private Switch switchLanguage;
    private TextView tvLanguageStatus;
    private SettingsViewModel settingsViewModel;
    private String authHeader;
    private boolean isBinding = false;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_language, container, false);

        switchLanguage = view.findViewById(R.id.switchLanguage);
        tvLanguageStatus = view.findViewById(R.id.tvLanguageStatus);

        settingsViewModel = new ViewModelProvider(
                this,
                new SettingsViewModelFactory(new SettingsRepository(requireContext()))
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();

        if (!TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }

        switchLanguage.setChecked(false);
        updateLanguageLabel(false);
        switchLanguage.setOnCheckedChangeListener(languageCheckedChangeListener);

        if (!TextUtils.isEmpty(authHeader)) {
            settingsViewModel.getLanguage(authHeader).observe(getViewLifecycleOwner(), language -> {
                boolean isVietnamese = "vi".equalsIgnoreCase(language);

                isBinding = true;
                switchLanguage.setChecked(isVietnamese);
                updateLanguageLabel(isVietnamese);
                isBinding = false;
            });
        }

        return view;
    }

    private final CompoundButton.OnCheckedChangeListener languageCheckedChangeListener =
            (buttonView, isChecked) -> {
                if (isBinding) return;

                String newLanguage = isChecked ? "vi" : "en";

                updateLanguageLabel(isChecked);
                applyLanguage(newLanguage);
                onLanguageSelected(newLanguage);
            };

    private void onLanguageSelected(String selectedLang) {
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        debounceRunnable = () -> {
            if (!TextUtils.isEmpty(authHeader)) {
                settingsViewModel.updateLanguage(authHeader, selectedLang, new SettingsViewModel.SettingsUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };

        debounceHandler.postDelayed(debounceRunnable, 500);
    }

    private void updateLanguageLabel(boolean isVietnamese) {
        tvLanguageStatus.setText(isVietnamese
                ? getString(R.string.language_current_vi)
                : getString(R.string.language_current_en));
    }

    private void applyLanguage(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        debounceHandler.removeCallbacksAndMessages(null);
    }
}