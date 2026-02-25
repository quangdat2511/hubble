package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityLoginBinding;
import com.example.hubble.view.MainActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private boolean isEmailMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupCountryCodePicker(); // Thêm hàm khởi tạo liên kết mã vùng
        setupTabs();
        setupClickListeners();
        observeViewModel();
    }

    private void setupCountryCodePicker() {
        // Liên kết CCP với EditText để tự động format và kiểm tra số điện thoại
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
                    binding.llPhoneContainer.setVisibility(View.GONE); // Đã sửa thành llPhoneContainer
                    binding.tvForgotPassword.setVisibility(View.VISIBLE);
                    binding.btnLogin.setText(R.string.login_btn);
                } else {
                    binding.tilEmail.setVisibility(View.GONE);
                    binding.tilPassword.setVisibility(View.GONE);
                    binding.llPhoneContainer.setVisibility(View.VISIBLE); // Đã sửa thành llPhoneContainer
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

        // Sử dụng thư viện CCP để kiểm tra tính hợp lệ của toàn bộ số (bao gồm cả độ dài theo từng quốc gia)
        if (!binding.ccp.isValidFullNumber()) {
            binding.tilPhone.setError(getString(R.string.error_invalid_phone));
            return;
        }

        // Tự động lấy số điện thoại đã gắn kèm mã vùng (ví dụ: +84912345678)
        String phone = binding.ccp.getFullNumberWithPlus();

        authViewModel.sendPhoneOtp(phone, this);
    }

    private void observeViewModel() {
        authViewModel.loginState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                authViewModel.loginState.setValue(null);
                navigateToMain();
            } else {
                setLoadingState(false);
                authViewModel.loginState.setValue(null);
                showError(result.getMessage());
            }
        });

        authViewModel.otpSendState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                String verificationId = result.getData();
                authViewModel.otpSendState.setValue(null);

                // Lấy lại số điện thoại đầy đủ để truyền sang OtpActivity
                String phone = binding.ccp.getFullNumberWithPlus();

                Intent intent = new Intent(this, OtpActivity.class);
                intent.putExtra(OtpActivity.EXTRA_PHONE_NUMBER, phone);
                intent.putExtra(OtpActivity.EXTRA_VERIFICATION_ID, verificationId);
                startActivity(intent);
            } else {
                setLoadingState(false);
                authViewModel.otpSendState.setValue(null);
                showError(result.getMessage());
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
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