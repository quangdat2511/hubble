package com.example.hubble.view.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityForgotPasswordBinding;
import com.example.hubble.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.tvBackToLogin.setOnClickListener(v -> finish());

        binding.btnSendReset.setOnClickListener(v -> handleSendReset());

        observeViewModel();
    }

    private void handleSendReset() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";

        binding.tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        authViewModel.sendPasswordResetEmail(email);
    }

    private void observeViewModel() {
        authViewModel.forgotPasswordState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                showSuccessState();
            } else {
                setLoadingState(false);
                showError(result.getMessage());
            }
        });
    }

    private void showSuccessState() {
        binding.layoutForm.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.VISIBLE);
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnSendReset.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(),
                message != null ? message : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }
}
