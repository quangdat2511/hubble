package com.example.hubble.view.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends BaseAuthActivity {

    private ActivitySettingsBinding binding;
    private AuthRepository authRepository;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        setupToolbar();
        setupRows();
        setupLogout();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRows() {
        View.OnClickListener comingSoon = v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show();

        binding.rowLanguage.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, LanguageSettingsActivity.class)));
        binding.rowNotifications.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, PushConfigSettingsActivity.class)));
        binding.rowAppearance.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, ThemeSettingsActivity.class)));

        binding.rowAdvanced.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, SessionManagementActivity.class)));

        binding.rowSupport.setOnClickListener(comingSoon);
        binding.rowChangelog.setOnClickListener(comingSoon);
    }

    private void setupLogout() {
        binding.cardLogout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.settings_logout_confirm_title))
                        .setMessage(getString(R.string.settings_logout_confirm_message))
                        .setNegativeButton(getString(R.string.settings_logout_confirm_no),
                                (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(getString(R.string.settings_logout_confirm_yes),
                                (dialog, which) -> {
                                    authRepository.logout();
                                    navigateToLogin();
                                })
                        .show());
    }
}
