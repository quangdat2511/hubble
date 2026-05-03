package com.example.hubble.view.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivityOtpBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

public class OtpActivity extends BaseAuthActivity {

    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_AUTO_SEND_OTP = "extra_auto_send_otp";

    private ActivityOtpBinding binding;
    private AuthViewModel authViewModel;
    private String phoneNumber;
    private String email;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private EditText[] otpFields;
    private boolean shouldAutoSendOtp = false;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        phoneNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        shouldAutoSendOtp = getIntent().getBooleanExtra(EXTRA_AUTO_SEND_OTP, false);

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        if (email != null) {
            binding.tvOtpSubtitle.setText(getString(R.string.otp_subtitle, email));
        } else if (phoneNumber != null) {
            binding.tvOtpSubtitle.setText(getString(R.string.otp_subtitle, phoneNumber));
        }

        otpFields = new EditText[]{
                binding.etOtp1, binding.etOtp2, binding.etOtp3,
                binding.etOtp4, binding.etOtp5, binding.etOtp6
        };

        setupOtpInputs();
        startCountDown();
        setupClickListeners();
        observeViewModel();
        
        // Auto-send OTP if coming from login with unverified email
        if (shouldAutoSendOtp) {
            if (email != null) {
                android.util.Log.d("OtpActivity", "Auto-sending OTP to email: " + email);
                authViewModel.sendEmailOtp(email);
            } else if (phoneNumber != null) {
                android.util.Log.d("OtpActivity", "Auto-sending OTP to phone: " + phoneNumber);
                authViewModel.sendPhoneOtp(phoneNumber);
            }
        }
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                    if (s.length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && otpFields[index].getText().toString().isEmpty()
                        && index > 0) {
                    otpFields[index - 1].requestFocus();
                    otpFields[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void startCountDown() {
        canResend = false;
        binding.tvResend.setEnabled(false);
        binding.tvResend.setText(getString(R.string.otp_resend, 60));

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                binding.tvResend.setText(getString(R.string.otp_resend, seconds));
            }

            @Override
            public void onFinish() {
                canResend = true;
                binding.tvResend.setEnabled(true);
                binding.tvResend.setText(R.string.otp_resend_active);
            }
        }.start();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnVerify.setOnClickListener(v -> {
            String otp = collectOtp();
            if (otp.length() < 6) {
                showError(getString(R.string.error_empty_otp));
                return;
            }
            if (email != null) {
                authViewModel.verifyEmailOtp(email, otp);
            } else if (phoneNumber != null) {
                authViewModel.verifyOtp(phoneNumber, otp);
            }
        });

        binding.tvResend.setOnClickListener(v -> {
            if (canResend) {
                if (email != null) {
                    authViewModel.sendEmailOtp(email);
                } else if (phoneNumber != null) {
                    authViewModel.resendPhoneOtp(phoneNumber);
                }
            }
        });
    }

    private void observeViewModel() {
        // Observe phone OTP send state
        authViewModel.otpSendState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                binding.tvResend.setEnabled(false);
            } else if (result.isSuccess()) {
                authViewModel.resetOtpSendState();
                clearOtpFields();
                startCountDown();
            } else {
                binding.tvResend.setEnabled(true);
                authViewModel.resetOtpSendState();
                showError(result.getMessage());
            }
        });

        // Observe email OTP send state
        authViewModel.emailOtpSendState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                binding.tvResend.setEnabled(false);
            } else if (result.isSuccess()) {
                authViewModel.resetEmailOtpSendState();
                clearOtpFields();
                startCountDown();
            } else {
                binding.tvResend.setEnabled(true);
                authViewModel.resetEmailOtpSendState();
                showError(result.getMessage());
            }
        });

        observeAuthResult(authViewModel.otpVerifyState,
                authViewModel::resetOtpVerifyState,
                this::navigateToMain);
    }

    private String collectOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText field : otpFields) {
            sb.append(field.getText().toString());
        }
        return sb.toString();
    }

    private void clearOtpFields() {
        for (EditText field : otpFields) {
            field.setText("");
        }
        otpFields[0].requestFocus();
    }

    @Override
    protected void setLoadingState(boolean isLoading) {
        super.setLoadingState(isLoading);
        binding.btnVerify.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}