package com.example.hubble.view.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivityForgotPasswordBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

public class ForgotPasswordActivity extends BaseAuthActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel authViewModel;
    private String currentEmail = "";

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnSendReset.setOnClickListener(v -> handleSendReset());
        binding.btnSubmitReset.setOnClickListener(v -> handleResetPassword());

        observeAuthResult(authViewModel.forgotPasswordState,
                authViewModel::resetForgotPasswordState,
                this::showResetFormState);

        observeAuthResult(authViewModel.resetPasswordState,
                authViewModel::resetResetPasswordState,
                this::showSuccessState);
    }

    private void handleSendReset() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        binding.tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        currentEmail = email;
        authViewModel.sendPasswordResetEmail(email);
    }

    private void handleResetPassword() {
        String otp = binding.etOtpReset.getText() != null ? binding.etOtpReset.getText().toString().trim() : "";
        String newPassword = binding.etNewPassword.getText() != null ? binding.etNewPassword.getText().toString() : "";

        binding.tilOtpReset.setError(null);
        binding.tilNewPassword.setError(null);

        if (TextUtils.isEmpty(otp) || otp.length() < 6) {
            binding.tilOtpReset.setError(getString(R.string.error_empty_otp));
            return;
        }
        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            binding.tilNewPassword.setError(getString(R.string.error_password_too_short));
            return;
        }

        authViewModel.resetPassword(currentEmail, otp, newPassword);
    }

    private void showResetFormState() {
        binding.layoutForm.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.GONE);
        binding.layoutResetPassword.setVisibility(View.VISIBLE);
    }

    private void showSuccessState() {
        binding.layoutForm.setVisibility(View.GONE);
        binding.layoutResetPassword.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setLoadingState(boolean isLoading) {
        super.setLoadingState(isLoading);
        binding.btnSendReset.setEnabled(!isLoading);
        binding.btnSubmitReset.setEnabled(!isLoading);
    }
}