package com.example.hubble.data.repository;

import android.app.Activity;

import androidx.annotation.NonNull;

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
                               RepositoryCallback<FirebaseUser> callback) {
        callback.onResult(AuthResult.loading());
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult ->
                        callback.onResult(AuthResult.success(authResult.getUser())))
                .addOnFailureListener(e ->
                        callback.onResult(AuthResult.error(e.getMessage())));
    }

    public void registerWithEmail(String email, String password, String username,
                                  RepositoryCallback<FirebaseUser> callback) {
        callback.onResult(AuthResult.loading());
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onResult(AuthResult.error("Không thể tạo tài khoản"));
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
                                        callback.onResult(AuthResult.success(user)))
                                .addOnFailureListener(e ->
                                        callback.onResult(AuthResult.error(e.getMessage())));
                    });
                })
                .addOnFailureListener(e ->
                        callback.onResult(AuthResult.error(e.getMessage())));
    }

    public void sendPhoneOtp(String phoneNumber, Activity activity,
                             PhoneAuthProvider.ForceResendingToken resendToken,
                             RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(createOtpCallbacks(callback));

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    public void verifyOtp(String verificationId, String code,
                          RepositoryCallback<FirebaseUser> callback) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential, callback);
    }

    public void sendPasswordResetEmail(String email,
                                       RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        callback.onResult(AuthResult.success(null)))
                .addOnFailureListener(e ->
                        callback.onResult(AuthResult.error(e.getMessage())));
    }

    public void logout() {
        mAuth.signOut();
    }


    // ─── Private Helpers ─────────────────────────────────────────

    private void signInWithCredential(PhoneAuthCredential credential,
                                      RepositoryCallback<FirebaseUser> callback) {
        callback.onResult(AuthResult.loading());
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult ->
                        callback.onResult(AuthResult.success(authResult.getUser())))
                .addOnFailureListener(e ->
                        callback.onResult(AuthResult.error(e.getMessage())));
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks createOtpCallbacks(
            RepositoryCallback<String> callback) {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // Auto-verification handled by Firebase
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                callback.onResult(AuthResult.error(e.getMessage()));
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                callback.onResult(AuthResult.success(verificationId));
            }
        };
    }
}
