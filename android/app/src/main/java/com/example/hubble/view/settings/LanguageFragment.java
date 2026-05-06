package com.example.hubble.view.settings;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.DeviceAlertSettingsRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.AppLanguageManager;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class LanguageFragment extends Fragment {

    private View rowEnglish;
    private View rowVietnamese;
    private TextView textEnglishLabel;
    private TextView textVietnameseLabel;
    private ImageView iconEnglishSelected;
    private ImageView iconVietnameseSelected;
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

        rowEnglish = view.findViewById(R.id.rowEnglish);
        rowVietnamese = view.findViewById(R.id.rowVietnamese);
        textEnglishLabel = view.findViewById(R.id.textEnglishLabel);
        textVietnameseLabel = view.findViewById(R.id.textVietnameseLabel);
        iconEnglishSelected = view.findViewById(R.id.iconEnglishSelected);
        iconVietnameseSelected = view.findViewById(R.id.iconVietnameseSelected);
        tvLanguageStatus = view.findViewById(R.id.tvLanguageStatus);

        settingsViewModel = new ViewModelProvider(
                this,
                new SettingsViewModelFactory(
                        new AuthRepository(requireContext()),
                        new SettingsRepository(requireContext()),
                        new PushConfigRepository(requireContext()),
                        new DeviceAlertSettingsRepository(requireContext()))
        ).get(SettingsViewModel.class);

        String token = new TokenManager(requireContext().getApplicationContext()).getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }

        bindLanguage(AppLanguageManager.getCurrentLanguage(requireContext()), false);
        setupRowActions();
        observeRemoteLanguage();

        return view;
    }

    private void setupRowActions() {
        rowEnglish.setOnClickListener(v -> onLanguageChosen(AppLanguageManager.LANGUAGE_ENGLISH));
        rowVietnamese.setOnClickListener(v -> onLanguageChosen(AppLanguageManager.LANGUAGE_VIETNAMESE));
    }

    private void observeRemoteLanguage() {
        if (TextUtils.isEmpty(authHeader)) {
            return;
        }

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

    private void onLanguageChosen(String newLanguage) {
        if (isBinding || newLanguage.equals(selectedLanguage)) {
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
        updateSelectionState();
        updateLanguageLabel(selectedLanguage);
        isBinding = false;

        if (applyLocale) {
            applyLanguageIfNeeded(selectedLanguage);
        }
    }

    private void updateSelectionState() {
        boolean englishSelected = AppLanguageManager.LANGUAGE_ENGLISH.equals(selectedLanguage);

        iconEnglishSelected.setImageResource(
                englishSelected ? R.drawable.ic_selection_checked : R.drawable.ic_selection_unchecked);
        iconVietnameseSelected.setImageResource(
                englishSelected ? R.drawable.ic_selection_unchecked : R.drawable.ic_selection_checked);

        textEnglishLabel.setAlpha(englishSelected ? 1f : 0.8f);
        textVietnameseLabel.setAlpha(englishSelected ? 0.8f : 1f);
    }

    private void updateLanguageLabel(String languageCode) {
        tvLanguageStatus.setText(AppLanguageManager.LANGUAGE_VIETNAMESE.equals(languageCode)
                ? getString(R.string.language_current_vi)
                : getString(R.string.language_current_en));
    }

    private void setLanguageSelectionEnabled(boolean enabled) {
        rowEnglish.setEnabled(enabled);
        rowVietnamese.setEnabled(enabled);
        rowEnglish.setAlpha(enabled ? 1f : 0.6f);
        rowVietnamese.setAlpha(enabled ? 1f : 0.6f);
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
