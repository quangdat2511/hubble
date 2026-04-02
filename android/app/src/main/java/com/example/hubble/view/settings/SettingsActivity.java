package com.example.hubble.view.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.settings.PushConfigResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends BaseAuthActivity {

    public static final String EXTRA_OPEN_PUSH_CONFIG = "extra_open_push_config";

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected View getRootView() {
        return binding.getRoot();
    }

    @Override
    protected View getProgressBar() {
        return binding.settingsLoadingIndicator;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new SettingsViewModelFactory(
                        new AuthRepository(this),
                        new SettingsRepository(this),
                        new PushConfigRepository(this)))
                .get(SettingsViewModel.class);

        setupToolbar();
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncVisibleContent);
        setupRows();
        setupPushConfigSummary();
        setupLogout();
        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_OPEN_PUSH_CONFIG, false)) {
            navigateTo(new PushConfigFragment(), false);
        }
        syncVisibleContent();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        });
    }

    private void setupRows() {
        View.OnClickListener comingSoon = v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show();

        binding.rowLanguage.setOnClickListener(comingSoon);
        binding.rowNotifications.setOnClickListener(v ->
                navigateTo(new PushConfigFragment(), true));
        binding.rowAppearance.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, ThemeActivity.class)));
        binding.rowAdvanced.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, SessionManagementActivity.class)));
        binding.rowSupport.setOnClickListener(comingSoon);
        binding.rowChangelog.setOnClickListener(comingSoon);
    }

    private void setupPushConfigSummary() {
        viewModel.currentPushConfig.observe(this, this::renderPushConfigSummary);
        renderPushConfigSummary(viewModel.getCurrentPushConfigValue());
        viewModel.loadPushConfig();
    }

    private void setupLogout() {
        binding.cardLogout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.settings_logout_confirm_title))
                        .setMessage(getString(R.string.settings_logout_confirm_message))
                        .setNegativeButton(getString(R.string.settings_logout_confirm_no),
                                (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(getString(R.string.settings_logout_confirm_yes),
                                (dialog, which) -> logoutAndNavigateToLogin())
                        .show());
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void updateTitle(@StringRes int titleRes) {
        binding.toolbar.setTitle(titleRes);
    }

    public void setScreenLoading(boolean isLoading) {
        setLoadingState(isLoading);
    }

    public void logoutAndNavigateToLogin() {
        viewModel.logout();
        navigateToLogin();
    }

    private void syncVisibleContent() {
        boolean showDetailScreen = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null;
        binding.settingsContent.setVisibility(showDetailScreen ? View.GONE : View.VISIBLE);
        binding.fragmentContainer.setVisibility(showDetailScreen ? View.VISIBLE : View.GONE);
        if (!showDetailScreen) {
            binding.toolbar.setTitle(R.string.settings_title);
        }
    }

    private void renderPushConfigSummary(PushConfigResponse config) {
        int summaryRes = R.string.settings_notifications_summary;
        if (config != null) {
            if (!config.isNotificationEnabled()) {
                summaryRes = R.string.settings_notifications_status_off;
            } else if (config.isNotificationSound()) {
                summaryRes = R.string.settings_notifications_status_on_sound;
            } else {
                summaryRes = R.string.settings_notifications_status_on_silent;
            }
        }
        binding.textNotificationsSummary.setText(summaryRes);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    public static Intent createIntent(Context context, boolean openPushConfig) {
        Intent intent = createIntent(context);
        intent.putExtra(EXTRA_OPEN_PUSH_CONFIG, openPushConfig);
        return intent;
    }
}
