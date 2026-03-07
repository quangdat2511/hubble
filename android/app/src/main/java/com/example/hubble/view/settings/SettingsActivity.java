package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends BaseAuthActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new SettingsViewModelFactory(new AuthRepository(this)))
                .get(SettingsViewModel.class);

        setupToolbar();
        setupRows();
        setupLogout();
    }

    private void setupToolbar() {
        // Use the left-arrow (ic_expand_more rotated) as a back button
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRows() {
        View.OnClickListener comingSoon = v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show();

        binding.rowLanguage.setOnClickListener(comingSoon);
        binding.rowNotifications.setOnClickListener(comingSoon);
        binding.rowAppearance.setOnClickListener(comingSoon);
        binding.rowAdvanced.setOnClickListener(comingSoon);
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
                                    viewModel.logout();
                                    navigateToLogin();
                                })
                        .show());
    }
}
