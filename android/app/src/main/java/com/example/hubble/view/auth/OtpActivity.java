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
    public static final String EXTRA_VERIFICATION_ID = "extra_verification_id";

    private ActivityOtpBinding binding;
    private AuthViewModel authViewModel;
    private String verificationId;
    private String phoneNumber;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private EditText[] otpFields;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        phoneNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
        verificationId = getIntent().getStringExtra(EXTRA_VERIFICATION_ID);

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository()))
                .get(AuthViewModel.class);

        binding.tvOtpSubtitle.setText(getString(R.string.otp_subtitle, phoneNumber));

        otpFields = new EditText[]{
                binding.etOtp1, binding.etOtp2, binding.etOtp3,
                binding.etOtp4, binding.etOtp5, binding.etOtp6
        };

        setupOtpInputs();
        startCountDown();
        setupClickListeners();
        observeViewModel();
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
            authViewModel.verifyOtp(otp, verificationId);
        });

        binding.tvResend.setOnClickListener(v -> {
            if (canResend && phoneNumber != null) {
                authViewModel.sendPhoneOtp(phoneNumber, this);
            }
        });
    }

    private void observeViewModel() {
        // OTP send (resend) observer — custom logic to update verificationId
        authViewModel.otpSendState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                binding.tvResend.setEnabled(false);
            } else if (result.isSuccess()) {
                verificationId = result.getData();
                authViewModel.resetOtpSendState();
                clearOtpFields();
                startCountDown();
            } else {
                binding.tvResend.setEnabled(true);
                authViewModel.resetOtpSendState();
                showError(result.getMessage());
            }
        });

        // OTP verify observer — standard pattern
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
