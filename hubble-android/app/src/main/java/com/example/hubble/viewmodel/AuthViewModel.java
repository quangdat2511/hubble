package com.example.hubble.viewmodel;

import android.app.Activity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    public final MutableLiveData<AuthResult<FirebaseUser>> loginState = new MutableLiveData<>();
    public final MutableLiveData<AuthResult<FirebaseUser>> registerState = new MutableLiveData<>();
    public final MutableLiveData<AuthResult<String>> otpSendState = new MutableLiveData<>();
    public final MutableLiveData<AuthResult<FirebaseUser>> otpVerifyState = new MutableLiveData<>();
    public final MutableLiveData<AuthResult<Void>> forgotPasswordState = new MutableLiveData<>();

    private String storedVerificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    public AuthViewModel() {
        repository = new AuthRepository();
    }

    public FirebaseUser getCurrentUser() {
        return repository.getCurrentUser();
    }

    public void loginWithEmail(String email, String password) {
        repository.loginWithEmail(email, password, loginState);
    }

    public void registerWithEmail(String email, String password, String username) {
        repository.registerWithEmail(email, password, username, registerState);
    }

    public void sendPhoneOtp(String phoneNumber, Activity activity) {
        MutableLiveData<AuthResult<String>> tempLiveData = new MutableLiveData<>();
        tempLiveData.observeForever(result -> {
            if (result != null && result.isSuccess()) {
                storedVerificationId = result.getData();
            }
            otpSendState.setValue(result);
        });
        repository.sendPhoneOtp(phoneNumber, activity, tempLiveData);
    }

    public void resendPhoneOtp(String phoneNumber, Activity activity) {
        if (resendToken == null) return;
        MutableLiveData<AuthResult<String>> tempLiveData = new MutableLiveData<>();
        tempLiveData.observeForever(result -> {
            if (result != null && result.isSuccess()) {
                storedVerificationId = result.getData();
            }
            otpSendState.setValue(result);
        });
        repository.resendPhoneOtp(phoneNumber, activity, resendToken, tempLiveData);
    }

    public void verifyOtp(String code, String verificationId) {
        if (verificationId == null) {
            otpVerifyState.setValue(AuthResult.error("Phiên xác thực hết hạn. Vui lòng gửi lại OTP."));
            return;
        }
        repository.verifyOtp(verificationId, code, otpVerifyState);
    }

    public void sendPasswordResetEmail(String email) {
        repository.sendPasswordResetEmail(email, forgotPasswordState);
    }

    public void logout() {
        repository.logout();
    }

    public String getStoredVerificationId() {
        return storedVerificationId;
    }
}
