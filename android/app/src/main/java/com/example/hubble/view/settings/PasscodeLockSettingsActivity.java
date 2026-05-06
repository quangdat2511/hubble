package com.example.hubble.view.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.settings.AppLockSettingsResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.DeviceAlertSettingsRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.databinding.ActivityPasscodeLockSettingsBinding;
import com.example.hubble.databinding.DialogPasscodeFormBinding;
import com.example.hubble.security.AppLockManager;
import com.example.hubble.security.AppLockRepository;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class PasscodeLockSettingsActivity extends BaseAuthActivity {

    private ActivityPasscodeLockSettingsBinding binding;
    private AppLockRepository repository;
    private SettingsViewModel viewModel;
    private boolean applyingState;
    private PendingPasscodeAction pendingAction;

    @Override
    protected View getRootView() {
        return binding.getRoot();
    }

    @Override
    protected View getProgressBar() {
        return binding.passcodeLoadingIndicator;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasscodeLockSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        repository = new AppLockRepository(this);
        viewModel = new ViewModelProvider(this,
                new SettingsViewModelFactory(
                        new AuthRepository(this),
                        new SettingsRepository(this),
                        new PushConfigRepository(this),
                        new DeviceAlertSettingsRepository(this)))
                .get(SettingsViewModel.class);
        setupToolbar();
        setupRows();
        setupObservers();
        renderState();
        viewModel.loadAppLockSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderState();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRows() {
        binding.rowPasscodeEnabled.setOnClickListener(v -> {
            if (!applyingState) {
                binding.switchPasscodeEnabled.toggle();
            }
        });

        binding.switchPasscodeEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (applyingState) {
                return;
            }
            if (isChecked) {
                handleEnableRequested();
            } else {
                repository.setPasscodeEnabled(false);
                AppLockManager manager = AppLockManager.getInstance();
                if (manager != null) {
                    manager.onUnlockSucceeded();
                }
                renderState();
                Snackbar.make(binding.getRoot(),
                        R.string.settings_passcode_saved_disabled,
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.rowChangePasscode.setOnClickListener(v -> showPasscodeDialog(
                PasscodeDialogMode.CHANGE,
                null
        ));
        binding.rowRemovePasscode.setOnClickListener(v -> showPasscodeDialog(
                PasscodeDialogMode.REMOVE,
                null
        ));
    }

    private void handleEnableRequested() {
        if (repository.hasStoredPin()) {
            repository.setPasscodeEnabled(true);
            AppLockManager manager = AppLockManager.getInstance();
            if (manager != null) {
                manager.onUnlockSucceeded();
            }
            renderState();
            Snackbar.make(binding.getRoot(),
                    R.string.settings_passcode_saved_enabled,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        showPasscodeDialog(PasscodeDialogMode.CREATE, () -> {
            repository.setPasscodeEnabled(false);
            renderState();
        });
    }

    private void setupObservers() {
        viewModel.appLockState.observe(this, this::handleLoadState);
        viewModel.appLockSaveState.observe(this, this::handleSaveState);
    }

    private void handleLoadState(AuthResult<AppLockSettingsResponse> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            setLoadingState(true);
            return;
        }

        setLoadingState(false);
        viewModel.resetAppLockState();
        if (result.isSuccess() && result.getData() != null) {
            applyRemoteSettings(result.getData());
            renderState();
            return;
        }

        if (result.isError()) {
            showError(result.getMessage());
            renderState();
        }
    }

    private void handleSaveState(AuthResult<AppLockSettingsResponse> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            setLoadingState(true);
            setPendingDialogLoading(true);
            return;
        }

        setLoadingState(false);
        setPendingDialogLoading(false);
        viewModel.resetAppLockSaveState();

        PendingPasscodeAction action = pendingAction;
        if (action == null) {
            if (result.isSuccess() && result.getData() != null) {
                applyRemoteSettings(result.getData());
                renderState();
            } else if (result.isError()) {
                showError(result.getMessage());
            }
            return;
        }

        if (result.isSuccess() && result.getData() != null) {
            applyRemoteSettings(result.getData());
            if (action.enablePasscodeAfterSuccess != null) {
                repository.setPasscodeEnabled(action.enablePasscodeAfterSuccess);
            }

            AppLockManager manager = AppLockManager.getInstance();
            if (manager != null) {
                manager.onUnlockSucceeded();
            }

            action.dialog.dismiss();
            pendingAction = null;
            renderState();
            Snackbar.make(binding.getRoot(), action.successMessageRes, Snackbar.LENGTH_SHORT).show();
            return;
        }

        showPendingActionError(action.dialogBinding, action.mode, result.getMessage());
    }

    private void renderState() {
        boolean hasPin = repository.hasStoredPin();
        boolean enabled = repository.isPasscodeEnabled();

        applyingState = true;
        binding.switchPasscodeEnabled.setChecked(enabled);
        binding.textPasscodeStatus.setText(enabled
                ? R.string.settings_passcode_lock_status_enabled
                : R.string.settings_passcode_lock_status_disabled);
        binding.textPasscodeStatusHint.setText(hasPin
                ? R.string.settings_passcode_timeout_hint
                : R.string.settings_passcode_lock_manage_description);
        binding.rowChangePasscode.setEnabled(hasPin);
        binding.rowChangePasscode.setAlpha(hasPin ? 1f : 0.5f);
        binding.rowRemovePasscode.setEnabled(hasPin);
        binding.rowRemovePasscode.setAlpha(hasPin ? 1f : 0.5f);
        binding.textPasscodeEnabledSummary.setText(hasPin
                ? R.string.settings_passcode_lock_toggle_summary
                : R.string.settings_passcode_lock_toggle_summary_empty);
        applyingState = false;
    }

    private void showPasscodeDialog(PasscodeDialogMode mode, @Nullable Runnable onCancel) {
        DialogPasscodeFormBinding dialogBinding = DialogPasscodeFormBinding.inflate(getLayoutInflater());
        configureDialog(mode, dialogBinding);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(mode.titleRes)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, (dialogInterface, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .setPositiveButton(mode.actionRes, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                clearErrors(dialogBinding);

                String currentPin = readPin(dialogBinding.inputCurrentPin);
                String newPin = readPin(dialogBinding.inputNewPin);
                String confirmPin = readPin(dialogBinding.inputConfirmPin);

                if (!validateDialog(mode, currentPin, newPin, confirmPin, dialogBinding)) {
                    return;
                }

                switch (mode) {
                    case CREATE:
                        submitPinUpdate(mode, newPin, dialog, dialogBinding,
                                R.string.settings_passcode_saved_enabled, true);
                        break;
                    case CHANGE:
                        submitPinUpdate(mode, newPin, dialog, dialogBinding,
                                R.string.settings_passcode_saved_changed, null);
                        break;
                    case REMOVE:
                    default:
                        submitPinUpdate(mode, null, dialog, dialogBinding,
                                R.string.settings_passcode_saved_removed, false);
                    return;
                }
            });
        });

        dialog.show();
    }

    private void configureDialog(PasscodeDialogMode mode, DialogPasscodeFormBinding dialogBinding) {
        boolean showCurrent = mode == PasscodeDialogMode.CHANGE || mode == PasscodeDialogMode.REMOVE;
        boolean showNew = mode != PasscodeDialogMode.REMOVE;

        dialogBinding.layoutCurrentPin.setVisibility(showCurrent ? View.VISIBLE : View.GONE);
        dialogBinding.layoutNewPin.setVisibility(showNew ? View.VISIBLE : View.GONE);
        dialogBinding.layoutConfirmPin.setVisibility(showNew ? View.VISIBLE : View.GONE);
        dialogBinding.layoutCurrentPin.setHelperText(mode == PasscodeDialogMode.REMOVE
                ? getString(R.string.settings_passcode_remove_summary)
                : null);
        dialogBinding.layoutNewPin.setHelperText(showNew
                ? getString(R.string.settings_passcode_pin_helper)
                : null);
    }

    private boolean validateDialog(PasscodeDialogMode mode,
                                   String currentPin,
                                   String newPin,
                                   String confirmPin,
                                   DialogPasscodeFormBinding dialogBinding) {
        if (mode == PasscodeDialogMode.CHANGE || mode == PasscodeDialogMode.REMOVE) {
            if (!isValidPin(currentPin)) {
                dialogBinding.layoutCurrentPin.setError(getString(R.string.settings_passcode_pin_error_invalid));
                return false;
            }
            if (!repository.verifyPin(currentPin)) {
                dialogBinding.layoutCurrentPin.setError(getString(R.string.settings_passcode_pin_error_current));
                return false;
            }
        }

        if (mode == PasscodeDialogMode.REMOVE) {
            return true;
        }

        if (!isValidPin(newPin)) {
            dialogBinding.layoutNewPin.setError(getString(R.string.settings_passcode_pin_error_invalid));
            return false;
        }

        if (!TextUtils.equals(newPin, confirmPin)) {
            dialogBinding.layoutConfirmPin.setError(getString(R.string.settings_passcode_pin_error_mismatch));
            return false;
        }

        if (mode == PasscodeDialogMode.CHANGE && TextUtils.equals(currentPin, newPin)) {
            dialogBinding.layoutNewPin.setError(getString(R.string.settings_passcode_pin_error_same));
            return false;
        }

        return true;
    }

    private void clearErrors(DialogPasscodeFormBinding dialogBinding) {
        dialogBinding.layoutCurrentPin.setError(null);
        dialogBinding.layoutNewPin.setError(null);
        dialogBinding.layoutConfirmPin.setError(null);
    }

    private boolean isValidPin(String value) {
        return value != null && value.matches("\\d{" + AppLockRepository.PIN_LENGTH + "}");
    }

    private String readPin(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void submitPinUpdate(PasscodeDialogMode mode,
                                 @Nullable String pin,
                                 androidx.appcompat.app.AlertDialog dialog,
                                 DialogPasscodeFormBinding dialogBinding,
                                 int successMessageRes,
                                 @Nullable Boolean enablePasscodeAfterSuccess) {
        pendingAction = new PendingPasscodeAction(
                mode,
                dialog,
                dialogBinding,
                successMessageRes,
                enablePasscodeAfterSuccess
        );
        viewModel.updateAppLockSettings(pin);
    }

    private void applyRemoteSettings(AppLockSettingsResponse response) {
        repository.syncPinFromServer(response != null ? response.getAppLockPin() : null);
    }

    private void setPendingDialogLoading(boolean isLoading) {
        if (pendingAction == null) {
            return;
        }

        Button positiveButton = pendingAction.dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = pendingAction.dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(!isLoading);
        }
        if (negativeButton != null) {
            negativeButton.setEnabled(!isLoading);
        }
        pendingAction.dialog.setCancelable(!isLoading);
        pendingAction.dialog.setCanceledOnTouchOutside(!isLoading);
    }

    private void showPendingActionError(DialogPasscodeFormBinding dialogBinding,
                                        PasscodeDialogMode mode,
                                        @Nullable String message) {
        pendingAction = null;
        if (mode == PasscodeDialogMode.REMOVE) {
            dialogBinding.layoutCurrentPin.setError(message != null
                    ? message
                    : getString(R.string.settings_passcode_pin_error_storage));
            return;
        }

        dialogBinding.layoutNewPin.setError(message != null
                ? message
                : getString(R.string.settings_passcode_pin_error_storage));
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, PasscodeLockSettingsActivity.class);
    }

    private enum PasscodeDialogMode {
        CREATE(R.string.settings_passcode_create_title, R.string.settings_passcode_create_action),
        CHANGE(R.string.settings_passcode_change_title, R.string.settings_passcode_change_action),
        REMOVE(R.string.settings_passcode_remove_title, R.string.settings_passcode_remove_action);

        private final int titleRes;
        private final int actionRes;

        PasscodeDialogMode(int titleRes, int actionRes) {
            this.titleRes = titleRes;
            this.actionRes = actionRes;
        }
    }

    private static final class PendingPasscodeAction {
        private final PasscodeDialogMode mode;
        private final androidx.appcompat.app.AlertDialog dialog;
        private final DialogPasscodeFormBinding dialogBinding;
        private final int successMessageRes;
        private final Boolean enablePasscodeAfterSuccess;

        private PendingPasscodeAction(PasscodeDialogMode mode,
                                      androidx.appcompat.app.AlertDialog dialog,
                                      DialogPasscodeFormBinding dialogBinding,
                                      int successMessageRes,
                                      @Nullable Boolean enablePasscodeAfterSuccess) {
            this.mode = mode;
            this.dialog = dialog;
            this.dialogBinding = dialogBinding;
            this.successMessageRes = successMessageRes;
            this.enablePasscodeAfterSuccess = enablePasscodeAfterSuccess;
        }
    }
}
