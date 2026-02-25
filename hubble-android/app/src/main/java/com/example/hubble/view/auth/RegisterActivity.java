package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityRegisterBinding;
import com.example.hubble.view.MainActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> handleRegister());

        binding.tvLogin.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void handleRegister() {
        String displayName = binding.etDisplayName.getText() != null
                ? binding.etDisplayName.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null
                ? binding.etConfirmPassword.getText().toString() : "";

        binding.tilDisplayName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(displayName)) {
            binding.tilDisplayName.setError(getString(R.string.error_empty_name));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        authViewModel.registerWithEmail(email, password, displayName);
    }

    private void observeViewModel() {
        authViewModel.registerState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                authViewModel.registerState.setValue(null);
                navigateToMain();
            } else {
                setLoadingState(false);
                authViewModel.registerState.setValue(null);
                showError(result.getMessage());
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(),
                message != null ? message : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }
}
