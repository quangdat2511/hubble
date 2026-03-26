package com.example.hubble.view.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;

import java.util.Locale;

public class LanguageFragment extends Fragment {

    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_VIETNAMESE = "vi";

    private RadioGroup languageGroup;
    private RadioButton radioEnglish;
    private RadioButton radioVietnamese;
    private TextView tvLanguageStatus;
    private SettingsViewModel settingsViewModel;
    private String authHeader;
    private String selectedLanguage = LANGUAGE_VIETNAMESE;
    private String pendingLanguageChange;
    private boolean isBinding = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_language, container, false);

        languageGroup = view.findViewById(R.id.languageGroup);
        radioEnglish = view.findViewById(R.id.radioEnglish);
        radioVietnamese = view.findViewById(R.id.radioVietnamese);
        tvLanguageStatus = view.findViewById(R.id.tvLanguageStatus);

        settingsViewModel = new ViewModelProvider(
                requireActivity()
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }

        bindLanguage(getCurrentLanguageCode(), false);
        languageGroup.setOnCheckedChangeListener(this::onLanguageChecked);

        if (!TextUtils.isEmpty(authHeader)) {
            settingsViewModel.getLanguage(authHeader).observe(getViewLifecycleOwner(), language -> {
                if (TextUtils.isEmpty(language)) {
                    return;
                }

                String normalizedLanguage = normalizeLanguage(language);
                if (!TextUtils.isEmpty(pendingLanguageChange)) {
                    if (pendingLanguageChange.equals(normalizedLanguage)) {
                        pendingLanguageChange = null;
                    }
                    return;
                }

                if (!normalizedLanguage.equals(selectedLanguage)) {
                    bindLanguage(normalizedLanguage, false);
                }
            });
        }

        return view;
    }

    private void onLanguageChecked(RadioGroup group, int checkedId) {
        if (isBinding) {
            return;
        }

        String newLanguage = checkedId == R.id.radioEnglish
                ? LANGUAGE_ENGLISH
                : LANGUAGE_VIETNAMESE;

        if (newLanguage.equals(selectedLanguage)) {
            return;
        }

        String previousLanguage = selectedLanguage;
        bindLanguage(newLanguage, false);

        if (TextUtils.isEmpty(authHeader)) {
            applyLanguageIfNeeded(newLanguage);
            return;
        }

        pendingLanguageChange = newLanguage;
        setLanguageSelectionEnabled(false);
        settingsViewModel.updateLanguage(authHeader, newLanguage, new SettingsViewModel.SettingsUpdateCallback() {
            @Override
            public void onSuccess() {
                pendingLanguageChange = null;
                setLanguageSelectionEnabled(true);
                applyLanguageIfNeeded(newLanguage);
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                pendingLanguageChange = null;
                setLanguageSelectionEnabled(true);
                bindLanguage(previousLanguage, false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindLanguage(String languageCode, boolean applyLocale) {
        selectedLanguage = normalizeLanguage(languageCode);

        isBinding = true;
        radioEnglish.setChecked(LANGUAGE_ENGLISH.equals(selectedLanguage));
        radioVietnamese.setChecked(LANGUAGE_VIETNAMESE.equals(selectedLanguage));
        updateLanguageLabel(selectedLanguage);
        isBinding = false;

        if (applyLocale) {
            applyLanguageIfNeeded(selectedLanguage);
        }
    }

    private void updateLanguageLabel(String languageCode) {
        tvLanguageStatus.setText(LANGUAGE_VIETNAMESE.equals(languageCode)
                ? getString(R.string.language_current_vi)
                : getString(R.string.language_current_en));
    }

    private void setLanguageSelectionEnabled(boolean enabled) {
        radioEnglish.setEnabled(enabled);
        radioVietnamese.setEnabled(enabled);
    }

    private void applyLanguageIfNeeded(String languageCode) {
        String currentLanguage = getCurrentLanguageCode();
        if (languageCode.equals(currentLanguage)) {
            return;
        }

        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    private String getCurrentLanguageCode() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty() && appLocales.get(0) != null) {
            return normalizeLanguage(appLocales.get(0).getLanguage());
        }

        Locale currentLocale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        return currentLocale != null
                ? normalizeLanguage(currentLocale.getLanguage())
                : LANGUAGE_VIETNAMESE;
    }

    private String normalizeLanguage(String languageCode) {
        if (languageCode == null) {
            return LANGUAGE_VIETNAMESE;
        }

        String normalized = languageCode.trim().toLowerCase(Locale.ROOT);
        return LANGUAGE_ENGLISH.equals(normalized) ? LANGUAGE_ENGLISH : LANGUAGE_VIETNAMESE;
    }
}
