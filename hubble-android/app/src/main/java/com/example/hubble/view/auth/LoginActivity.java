package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivityLoginBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.tabs.TabLayout;

public class LoginActivity extends BaseAuthActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private boolean isEmailMode = true;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository()))
                .get(AuthViewModel.class);

        setupCountryCodePicker();
        setupTabs();
        setupClickListeners();
        observeViewModel();
    }

    private void setupCountryCodePicker() {
        binding.ccp.registerCarrierNumberEditText(binding.etPhone);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isEmailMode = tab.getPosition() == 0;
                if (isEmailMode) {
                    binding.tilEmail.setVisibility(View.VISIBLE);
                    binding.tilPassword.setVisibility(View.VISIBLE);
                    binding.llPhoneContainer.setVisibility(View.GONE);
                    binding.tvForgotPassword.setVisibility(View.VISIBLE);
                    binding.btnLogin.setText(R.string.login_btn);
                } else {
                    binding.tilEmail.setVisibility(View.GONE);
                    binding.tilPassword.setVisibility(View.GONE);
                    binding.llPhoneContainer.setVisibility(View.VISIBLE);
                    binding.tvForgotPassword.setVisibility(View.GONE);
                    binding.btnLogin.setText(R.string.login_send_otp);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            if (isEmailMode) {
                handleEmailLogin();
            } else {
                handlePhoneLogin();
            }
        });

        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleEmailLogin() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

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

        authViewModel.loginWithEmail(email, password);
    }

    private void handlePhoneLogin() {
        binding.tilPhone.setError(null);

        if (!binding.ccp.isValidFullNumber()) {
            binding.tilPhone.setError(getString(R.string.error_invalid_phone));
            return;
        }

        String phone = binding.ccp.getFullNumberWithPlus();
        authViewModel.sendPhoneOtp(phone, this);
    }

    private void observeViewModel() {
        // Email login observer
        observeAuthResult(authViewModel.loginState,
                authViewModel::resetLoginState,
                this::navigateToMain);

        // OTP send observer
        authViewModel.otpSendState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                String verificationId = result.getData();
                authViewModel.resetOtpSendState();

                String phone = binding.ccp.getFullNumberWithPlus();
                Intent intent = new Intent(this, OtpActivity.class);
                intent.putExtra(OtpActivity.EXTRA_PHONE_NUMBER, phone);
                intent.putExtra(OtpActivity.EXTRA_VERIFICATION_ID, verificationId);
                startActivity(intent);
            } else {
                setLoadingState(false);
                authViewModel.resetOtpSendState();
                showError(result.getMessage());
            }
        });
    }

    @Override
    protected void setLoadingState(boolean isLoading) {
        super.setLoadingState(isLoading);
        binding.btnLogin.setEnabled(!isLoading);
    }
}