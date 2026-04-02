package com.example.hubble.view.settings;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.AppLanguageManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class LanguageFragment extends Fragment {

    private RadioGroup languageGroup;
    private RadioButton radioEnglish;
    private RadioButton radioVietnamese;
    private TextView tvLanguageStatus;
    private SettingsViewModel settingsViewModel;
    private String authHeader;
    private String selectedLanguage = AppLanguageManager.DEFAULT_LANGUAGE;
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
                this,
                new SettingsViewModelFactory(
                        new AuthRepository(requireContext()),
                        new SettingsRepository(requireContext()),
                        new PushConfigRepository(requireContext()))
        ).get(SettingsViewModel.class);

        TokenManager tokenManager = new TokenManager(requireContext().getApplicationContext());
        String token = tokenManager.getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }

        bindLanguage(AppLanguageManager.getCurrentLanguage(requireContext()), false);
        languageGroup.setOnCheckedChangeListener(this::onLanguageChecked);

        if (!TextUtils.isEmpty(authHeader)) {
            settingsViewModel.getLanguage(authHeader).observe(getViewLifecycleOwner(), language -> {
                if (TextUtils.isEmpty(language)) {
                    return;
                }

                String normalizedLanguage = AppLanguageManager.normalize(language);
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
                ? AppLanguageManager.LANGUAGE_ENGLISH
                : AppLanguageManager.LANGUAGE_VIETNAMESE;

        if (newLanguage.equals(selectedLanguage)) {
            return;
        }

        String previousLanguage = selectedLanguage;
        bindLanguage(newLanguage, false);

        if (TextUtils.isEmpty(authHeader)) {
            applyLanguageIfNeeded(newLanguage);
            refreshScreenWithSuccess();
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
                refreshScreenWithSuccess();
            }
 
            @Override
            public void onError(String message) {
                pendingLanguageChange = null;
                setLanguageSelectionEnabled(true);
                bindLanguage(previousLanguage, false);
                InAppMessageUtils.show(LanguageFragment.this, message);
            }
        });
    }

    private void bindLanguage(String languageCode, boolean applyLocale) {
        selectedLanguage = AppLanguageManager.normalize(languageCode);

        isBinding = true;
        radioEnglish.setChecked(AppLanguageManager.LANGUAGE_ENGLISH.equals(selectedLanguage));
        radioVietnamese.setChecked(AppLanguageManager.LANGUAGE_VIETNAMESE.equals(selectedLanguage));
        updateLanguageLabel(selectedLanguage);
        isBinding = false;

        if (applyLocale) {
            applyLanguageIfNeeded(selectedLanguage);
        }
    }

    private void updateLanguageLabel(String languageCode) {
        tvLanguageStatus.setText(AppLanguageManager.LANGUAGE_VIETNAMESE.equals(languageCode)
                ? getString(R.string.language_current_vi)
                : getString(R.string.language_current_en));
    }

    private void setLanguageSelectionEnabled(boolean enabled) {
        radioEnglish.setEnabled(enabled);
        radioVietnamese.setEnabled(enabled);
    }

    private void applyLanguageIfNeeded(String languageCode) {
        String currentLanguage = AppLanguageManager.getCurrentLanguage(requireContext());
        if (languageCode.equals(currentLanguage)) {
            return;
        }

        AppLanguageManager.applyAppLanguage(languageCode);
    }

    private void refreshScreenWithSuccess() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.setResult(Activity.RESULT_OK);
        if (activity instanceof LanguageSettingsActivity) {
            ((LanguageSettingsActivity) activity).relaunchForLocaleChange(true);
            return;
        }

        activity.recreate();
    }
}
