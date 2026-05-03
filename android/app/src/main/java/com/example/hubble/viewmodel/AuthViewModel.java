package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<AuthResult<UserResponse>> _loginState = new MutableLiveData<>();
    public final LiveData<AuthResult<UserResponse>> loginState = _loginState;

    private final MutableLiveData<AuthResult<String>> _registerState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> registerState = _registerState;

    private final MutableLiveData<AuthResult<String>> _otpSendState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> otpSendState = _otpSendState;

    private final MutableLiveData<AuthResult<String>> _emailOtpSendState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> emailOtpSendState = _emailOtpSendState;

    private final MutableLiveData<AuthResult<UserResponse>> _otpVerifyState = new MutableLiveData<>();
    public final LiveData<AuthResult<UserResponse>> otpVerifyState = _otpVerifyState;

    private final MutableLiveData<AuthResult<String>> _forgotPasswordState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> forgotPasswordState = _forgotPasswordState;

    private final MutableLiveData<AuthResult<String>> _resetPasswordState = new MutableLiveData<>();
    public final LiveData<AuthResult<String>> resetPasswordState = _resetPasswordState;

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public UserResponse getCurrentUser() {
        return repository.getCurrentUser();
    }

    public void loginWithEmail(String email, String password) {
        repository.loginWithEmail(email, password, result -> _loginState.setValue(result));
    }

    public void loginWithGoogle(String idToken) {
        repository.loginWithGoogle(idToken, result -> _loginState.setValue(result));
    }

    public void registerWithEmail(String email, String password, String username) {
        repository.registerWithEmail(email, password, username, result -> _registerState.postValue(result));
    }

    public void verifyEmailOtp(String email, String otpCode) {
        repository.verifyEmailOtp(email, otpCode, result -> _otpVerifyState.postValue(result));
    }

    public void sendEmailOtp(String email) {
        repository.sendEmailOtp(email, result -> _emailOtpSendState.setValue(result));
    }

    public void sendPhoneOtp(String phoneNumber) {
        repository.sendPhoneOtp(phoneNumber, result -> _otpSendState.setValue(result));
    }

    public void resendPhoneOtp(String phoneNumber) {
        sendPhoneOtp(phoneNumber);
    }

    public void verifyOtp(String phone, String code) {
        repository.verifyOtp(phone, code, result -> _otpVerifyState.setValue(result));
    }

    public void sendPasswordResetEmail(String email) {
        repository.sendPasswordResetEmail(email, result -> _forgotPasswordState.setValue(result));
    }

    public void resetPassword(String email, String otpCode, String newPassword) {
        repository.resetPassword(email, otpCode, newPassword, result -> _resetPasswordState.postValue(result));
    }

    public void logout() {
        repository.logout();
    }

    public void resetLoginState() { _loginState.setValue(null); }
    public void resetRegisterState() { _registerState.setValue(null); }
    public void resetOtpSendState() { _otpSendState.setValue(null); }
    public void resetEmailOtpSendState() { _emailOtpSendState.setValue(null); }
    public void resetOtpVerifyState() { _otpVerifyState.setValue(null); }
    public void resetForgotPasswordState() { _forgotPasswordState.setValue(null); }
    public void resetResetPasswordState() { _resetPasswordState.setValue(null); }
}