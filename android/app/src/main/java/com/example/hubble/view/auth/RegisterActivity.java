package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivityRegisterBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

import java.util.regex.Pattern;

public class RegisterActivity extends BaseAuthActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private String currentEmail = "";

    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$";

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> handleRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
        binding.btnGoLogin.setOnClickListener(v -> navigateToLogin());
        binding.btnRetry.setOnClickListener(v -> showFormState());

        observeAuthResult(authViewModel.registerState,
                authViewModel::resetRegisterState,
                this::navigateToOtp,
                this::showErrorState);
    }

    private void handleRegister() {
        String displayName = binding.etDisplayName.getText() != null ? binding.etDisplayName.getText().toString().trim() : "";
        currentEmail = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";

        binding.tilDisplayName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(displayName)) {
            binding.tilDisplayName.setError(getString(R.string.error_empty_name));
            return;
        }
        if (TextUtils.isEmpty(currentEmail)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
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

        if (!Pattern.compile(PASSWORD_PATTERN).matcher(password).matches()) {
            binding.tilPassword.setError(getString(R.string.error_password_weak));
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        authViewModel.registerWithEmail(currentEmail, password, displayName);
    }

    private void navigateToOtp() {
        Intent intent = new Intent(this, OtpActivity.class);
        intent.putExtra(OtpActivity.EXTRA_EMAIL, currentEmail);
        startActivity(intent);
        finish();
    }

    private void showErrorState(String message) {
        binding.layoutForm.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(message != null ? message : getString(R.string.error_generic));
    }

    private void showFormState() {
        binding.layoutSuccess.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutForm.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setLoadingState(boolean isLoading) {
        super.setLoadingState(isLoading);
        binding.btnRegister.setEnabled(!isLoading);
    }
}