package com.example.hubble.viewmodel;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<AuthResult<FirebaseUser>> _loginState = new MutableLiveData<>();
    public final LiveData<AuthResult<FirebaseUser>> loginState = _loginState;

    private final MutableLiveData<AuthResult<FirebaseUser>> _registerState = new MutableLiveData<>();
    public final LiveData<AuthResult<FirebaseUser>> registerState = _registerState;

    private final MutableLiveData<AuthResult<String>> _otpSendState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> otpSendState = _otpSendState;

    private final MutableLiveData<AuthResult<FirebaseUser>> _otpVerifyState = new MutableLiveData<>();
    public final LiveData<AuthResult<FirebaseUser>> otpVerifyState = _otpVerifyState;

    private final MutableLiveData<AuthResult<Void>> _forgotPasswordState = new MutableLiveData<>();
    public final LiveData<AuthResult<Void>> forgotPasswordState = _forgotPasswordState;

    private String storedVerificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public FirebaseUser getCurrentUser() {
        return repository.getCurrentUser();
    }

    // ─── Auth Actions ────────────────────────────────────────────

    public void loginWithEmail(String email, String password) {
        repository.loginWithEmail(email, password, result -> _loginState.setValue(result));
    }

    public void registerWithEmail(String email, String password, String username) {
        repository.registerWithEmail(email, password, username, result -> _registerState.postValue(result));
    }

    /**
     * Send OTP to phone number. Activity is required by Firebase SDK for reCAPTCHA.
     */
    public void sendPhoneOtp(String phoneNumber, Activity activity) {
        repository.sendPhoneOtp(phoneNumber, activity, null, result -> {
            if (result != null && result.isSuccess()) {
                storedVerificationId = result.getData();
            }
            _otpSendState.setValue(result);
        });
    }

    public void resendPhoneOtp(String phoneNumber, Activity activity) {
        if (resendToken == null) return;
        repository.sendPhoneOtp(phoneNumber, activity, resendToken, result -> {
            if (result != null && result.isSuccess()) {
                storedVerificationId = result.getData();
            }
            _otpSendState.setValue(result);
        });
    }

    public void verifyOtp(String code, String verificationId) {
        if (verificationId == null) {
            _otpVerifyState.setValue(AuthResult.error("Phiên xác thực hết hạn. Vui lòng gửi lại OTP."));
            return;
        }
        repository.verifyOtp(verificationId, code, result -> _otpVerifyState.setValue(result));
    }

    public void sendPasswordResetEmail(String email) {
        repository.sendPasswordResetEmail(email, result -> _forgotPasswordState.setValue(result));
    }

    public void logout() {
        repository.logout();
    }

    // ─── State Accessors ─────────────────────────────────────────

    public String getStoredVerificationId() {
        return storedVerificationId;
    }

    // ─── State Reset (called by BaseAuthActivity observer helper) ─

    public void resetLoginState() { _loginState.setValue(null); }
    public void resetRegisterState() { _registerState.setValue(null); }
    public void resetOtpSendState() { _otpSendState.setValue(null); }
    public void resetOtpVerifyState() { _otpVerifyState.setValue(null); }
    public void resetForgotPasswordState() { _forgotPasswordState.setValue(null); }
}
