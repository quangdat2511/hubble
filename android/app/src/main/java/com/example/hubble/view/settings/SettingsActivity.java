package com.example.hubble.view.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.hubble.security.AppLockRepository;
import com.example.hubble.security.AppSwitcherProtectionManager;
import com.example.hubble.security.AppSwitcherProtectionRepository;
import com.example.hubble.utils.AppLanguageManager;
import com.example.hubble.utils.ThemeManager;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.view.me.QrHubActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends BaseAuthActivity {

    public static final String EXTRA_OPEN_PUSH_CONFIG = "extra_open_push_config";

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private AppLockRepository appLockRepository;
    private AppSwitcherProtectionRepository appSwitcherProtectionRepository;
    private boolean isApplyingSecurityToggle;
    private final ActivityResultLauncher<Intent> languageSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK);
                    recreate();
                }
            });

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
        appLockRepository = new AppLockRepository(this);
        appSwitcherProtectionRepository = new AppSwitcherProtectionRepository(this);

        viewModel = new ViewModelProvider(this,
                new SettingsViewModelFactory(
                        new AuthRepository(this),
                        new SettingsRepository(this),
                        new PushConfigRepository(this)))
                .get(SettingsViewModel.class);

        setupToolbar();
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncVisibleContent);
        setupRows();
        renderStaticSummaries();
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

        binding.rowLanguage.setOnClickListener(v ->
                languageSettingsLauncher.launch(new Intent(this, LanguageSettingsActivity.class)));
        binding.rowQr.setOnClickListener(v ->
                startActivity(new Intent(this, QrHubActivity.class)));
        binding.rowNotifications.setOnClickListener(v ->
                navigateTo(new PushConfigFragment(), true));
        binding.rowAppearance.setOnClickListener(v ->
                startActivity(new Intent(this, ThemeActivity.class)));
        binding.rowPasscodeLock.setOnClickListener(v ->
                startActivity(PasscodeLockSettingsActivity.createIntent(this)));
        binding.rowAppSwitcherProtection.setOnClickListener(v -> {
            if (!isApplyingSecurityToggle) {
                binding.switchAppSwitcherProtection.toggle();
            }
        });
        binding.switchAppSwitcherProtection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isApplyingSecurityToggle) {
                return;
            }

            appSwitcherProtectionRepository.setEnabled(isChecked);
            AppSwitcherProtectionManager manager = AppSwitcherProtectionManager.getInstance();
            if (manager != null) {
                manager.refreshProtection();
            }
            renderSecuritySummary();
            Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
        });
        binding.rowAdvanced.setOnClickListener(v ->
                startActivity(new Intent(this, SessionManagementActivity.class)));
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

    @Override
    protected void onResume() {
        super.onResume();
        renderStaticSummaries();
        renderSecuritySummary();
    }

    private void renderStaticSummaries() {
        String languageCode = AppLanguageManager.getCurrentLanguage(this);
        binding.textLanguageSummary.setText(
                AppLanguageManager.LANGUAGE_ENGLISH.equals(languageCode)
                        ? R.string.language_current_en
                        : R.string.language_current_vi
        );

        String savedTheme = ThemeManager.getSavedTheme(this);
        int themeSummaryRes;
        switch (savedTheme) {
            case ThemeManager.THEME_LIGHT:
                themeSummaryRes = R.string.theme_light;
                break;
            case ThemeManager.THEME_DARK:
                themeSummaryRes = R.string.theme_dark;
                break;
            default:
                themeSummaryRes = R.string.theme_system;
                break;
        }
        binding.textAppearanceSummary.setText(themeSummaryRes);
        binding.textPasscodeLockSummary.setText(getPasscodeSummaryRes());
    }

    private int getPasscodeSummaryRes() {
        if (appLockRepository.isPasscodeEnabled()) {
            return R.string.settings_passcode_lock_summary_enabled;
        }
        if (appLockRepository.hasStoredPin()) {
            return R.string.settings_passcode_lock_summary_configured;
        }
        return R.string.settings_passcode_lock_summary_disabled;
    }

    private void renderSecuritySummary() {
        boolean isEnabled = appSwitcherProtectionRepository.isEnabled();
        isApplyingSecurityToggle = true;
        binding.switchAppSwitcherProtection.setChecked(isEnabled);
        binding.textAppSwitcherProtectionSummary.setText(isEnabled
                ? R.string.settings_app_switcher_protection_summary_on
                : R.string.settings_app_switcher_protection_summary_off);
        isApplyingSecurityToggle = false;
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
