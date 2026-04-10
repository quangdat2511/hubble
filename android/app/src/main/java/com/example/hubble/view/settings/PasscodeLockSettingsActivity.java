package com.example.hubble.view.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityPasscodeLockSettingsBinding;
import com.example.hubble.databinding.DialogPasscodeFormBinding;
import com.example.hubble.security.AppLockManager;
import com.example.hubble.security.AppLockRepository;
import com.example.hubble.view.base.BaseAuthActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class PasscodeLockSettingsActivity extends BaseAuthActivity {

    private ActivityPasscodeLockSettingsBinding binding;
    private AppLockRepository repository;
    private boolean applyingState;

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
        setupToolbar();
        setupRows();
        renderState();
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
                repository.clearBackgroundState();
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
            repository.clearBackgroundState();
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

                boolean success;
                int successMessageRes;
                switch (mode) {
                    case CREATE:
                        success = repository.savePin(newPin);
                        if (success) {
                            repository.setPasscodeEnabled(true);
                            repository.clearBackgroundState();
                        }
                        successMessageRes = R.string.settings_passcode_saved_enabled;
                        break;
                    case CHANGE:
                        success = repository.updatePin(newPin);
                        if (success) {
                            repository.clearBackgroundState();
                        }
                        successMessageRes = R.string.settings_passcode_saved_changed;
                        break;
                    case REMOVE:
                    default:
                        success = true;
                        repository.clearPin();
                        repository.clearBackgroundState();
                        successMessageRes = R.string.settings_passcode_saved_removed;
                        break;
                }

                if (!success) {
                    dialogBinding.layoutNewPin.setError(getString(R.string.settings_passcode_pin_error_storage));
                    return;
                }

                AppLockManager manager = AppLockManager.getInstance();
                if (manager != null) {
                    manager.onUnlockSucceeded();
                }
                dialog.dismiss();
                renderState();
                Snackbar.make(binding.getRoot(), successMessageRes, Snackbar.LENGTH_SHORT).show();
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
}
