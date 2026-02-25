package com.example.hubble.data.repository;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.UserModel;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class AuthRepository {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;

    public AuthRepository() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void loginWithEmail(String email, String password,
                               MutableLiveData<AuthResult<FirebaseUser>> resultLiveData) {
        resultLiveData.setValue(AuthResult.loading());
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult ->
                        resultLiveData.setValue(AuthResult.success(authResult.getUser())))
                .addOnFailureListener(e ->
                        resultLiveData.setValue(AuthResult.error(e.getMessage())));
    }

    public void registerWithEmail(String email, String password, String username,
                                  MutableLiveData<AuthResult<FirebaseUser>> resultLiveData) {
        resultLiveData.setValue(AuthResult.loading());
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        resultLiveData.setValue(AuthResult.error("Không thể tạo tài khoản"));
                        return;
                    }
                    
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build();
                    user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                        UserModel userModel = new UserModel(user.getUid(), username, email, null);
                        mFirestore.collection("users")
                                .document(user.getUid())
                                .set(userModel)
                                .addOnSuccessListener(unused ->
                                        resultLiveData.setValue(AuthResult.success(user)))
                                .addOnFailureListener(e ->
                                        resultLiveData.setValue(AuthResult.error(e.getMessage())));
                    });
                })
                .addOnFailureListener(e ->
                        resultLiveData.setValue(AuthResult.error(e.getMessage())));
    }

    public void sendPhoneOtp(String phoneNumber, Activity activity,
                             MutableLiveData<AuthResult<String>> verificationLiveData) {
        verificationLiveData.setValue(AuthResult.loading());
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithCredential(credential,
                                new MutableLiveData<>());
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        verificationLiveData.setValue(AuthResult.error(e.getMessage()));
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationLiveData.setValue(AuthResult.success(verificationId));
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void resendPhoneOtp(String phoneNumber, Activity activity,
                               PhoneAuthProvider.ForceResendingToken resendToken,
                               MutableLiveData<AuthResult<String>> verificationLiveData) {
        verificationLiveData.setValue(AuthResult.loading());
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {}

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        verificationLiveData.setValue(AuthResult.error(e.getMessage()));
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationLiveData.setValue(AuthResult.success(verificationId));
                    }
                })
                .setForceResendingToken(resendToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void verifyOtp(String verificationId, String code,
                          MutableLiveData<AuthResult<FirebaseUser>> resultLiveData) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential, resultLiveData);
    }

    private void signInWithCredential(PhoneAuthCredential credential,
                                      MutableLiveData<AuthResult<FirebaseUser>> resultLiveData) {
        resultLiveData.setValue(AuthResult.loading());
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult ->
                        resultLiveData.setValue(AuthResult.success(authResult.getUser())))
                .addOnFailureListener(e ->
                        resultLiveData.setValue(AuthResult.error(e.getMessage())));
    }

    public void sendPasswordResetEmail(String email,
                                       MutableLiveData<AuthResult<Void>> resultLiveData) {
        resultLiveData.setValue(AuthResult.loading());
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        resultLiveData.setValue(AuthResult.success(null)))
                .addOnFailureListener(e ->
                        resultLiveData.setValue(AuthResult.error(e.getMessage())));
    }

    public void logout() {
        mAuth.signOut();
    }
}
