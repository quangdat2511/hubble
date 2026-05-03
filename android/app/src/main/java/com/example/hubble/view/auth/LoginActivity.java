package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.NotificationRepository;
import com.example.hubble.databinding.ActivityLoginBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends BaseAuthActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            authViewModel.loginWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        showError("Google sign in failed: " + e.getStatusCode());
                    }
                }
            }
    );

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.progressBar; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Keep the login screen in dark mode so it never falls back to the light auth palette.
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        setupGoogleSignIn();
        setupUI();
        setupClickListeners();
        observeViewModel();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupUI() {
        binding.tabLayout.setVisibility(View.GONE);
        binding.llPhoneContainer.setVisibility(View.GONE);
        binding.tilEmail.setVisibility(View.VISIBLE);
        binding.tilPassword.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> handleEmailLogin());
        binding.btnGoogleLogin.setOnClickListener(v -> handleGoogleLogin());
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleEmailLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

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

    private void handleGoogleLogin() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void observeViewModel() {
        authViewModel.loginState.observe(this, result -> {
            if (result == null) return;
            
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                authViewModel.resetLoginState();
                registerDeviceTokenAndNavigate();
            } else {
                setLoadingState(false);
                authViewModel.resetLoginState();
                
                // Check if this is an EMAIL_NOT_VERIFIED error (code 1020)
                if (result.getErrorCode() == 1020) {
                    String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
                    if (!email.isEmpty()) {
                        Intent intent = new Intent(this, OtpActivity.class);
                        intent.putExtra(OtpActivity.EXTRA_EMAIL, email);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
                
                showError(result.getMessage());
            }
        });
    }

    private void registerDeviceTokenAndNavigate() {
        // After login succeeds, immediately register device token
        NotificationRepository notificationRepo = new NotificationRepository(this);
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> {
                    android.util.Log.d("LoginActivity", "Got FCM token after login: " + token);
                    notificationRepo.registerDeviceToken(token);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LoginActivity", "Failed to get FCM token: " + e.getMessage());
                    navigateToMain();
                });
    }

    @Override
    protected void setLoadingState(boolean isLoading) {
        super.setLoadingState(isLoading);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoogleLogin.setEnabled(!isLoading);
    }
}
