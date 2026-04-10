package com.example.hubble.view.security;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityAppLockBinding;
import com.example.hubble.security.AppLockManager;
import com.example.hubble.security.AppLockRepository;

public class AppLockActivity extends AppCompatActivity {

    private ActivityAppLockBinding binding;
    private AppLockRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new AppLockRepository(this);
        if (!repository.hasStoredPin()) {
            repository.setPasscodeEnabled(false);
        }
        if (!repository.isPasscodeEnabled() || !repository.hasStoredPin()) {
            finish();
            return;
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        binding.buttonUnlock.setOnClickListener(v -> submitPin());
        binding.buttonExit.setOnClickListener(v -> moveTaskToBack(true));
    }

    private void submitPin() {
        String pin = binding.inputPasscode.getText() == null
                ? ""
                : binding.inputPasscode.getText().toString().trim();

        binding.layoutPasscode.setError(null);
        if (!pin.matches("\\d{" + AppLockRepository.PIN_LENGTH + "}")) {
            binding.layoutPasscode.setError(getString(R.string.settings_passcode_pin_error_invalid));
            return;
        }

        if (!repository.verifyPin(pin)) {
            binding.inputPasscode.setText(null);
            binding.layoutPasscode.setError(getString(R.string.settings_passcode_prompt_error));
            return;
        }

        AppLockManager manager = AppLockManager.getInstance();
        if (manager != null) {
            manager.onUnlockSucceeded();
        } else {
            repository.clearBackgroundState();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(0, 0);
        }
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, AppLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }
}
