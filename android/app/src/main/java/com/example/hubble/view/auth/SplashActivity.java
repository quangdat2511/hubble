package com.example.hubble.view.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.BuildConfig;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivitySplashBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

public class SplashActivity extends BaseAuthActivity {

    private ActivitySplashBinding binding;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        AuthViewModel authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);
        
        TokenManager tokenManager = new TokenManager(this);
        tokenManager.clearSessionIfBaseUrlChanged(BuildConfig.BASE_URL);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (authViewModel.getCurrentUser() != null) {
                navigateToMain();
            } else {
                navigateToLogin();
            }
        }, 1800);
    }
}