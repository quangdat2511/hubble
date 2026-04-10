package com.example.hubble.view.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.AppLockSyncManager;
import com.example.hubble.databinding.ActivitySplashBinding;
import com.example.hubble.utils.AppLanguageManager;
import com.example.hubble.utils.ThemeSyncManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

public class SplashActivity extends BaseAuthActivity {

    private static final long SPLASH_DELAY_MILLIS = 1800L;

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated;

    @Override
    protected View getRootView() {
        return binding.getRoot();
    }

    @Override
    protected View getProgressBar() {
        return binding.getRoot();
    }

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
        tokenManager.clearSessionIfBaseUrlChanged(NetworkConfig.getBackendIdentity());

        boolean isLoggedIn = authViewModel.getCurrentUser() != null;
        scheduleNavigation(isLoggedIn);

        if (!isLoggedIn) {
            return;
        }

        String accessToken = tokenManager.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            ThemeSyncManager.syncThemeIfAuthenticated(this,
                    () -> AppLockSyncManager.syncAppLockIfAuthenticated(this, () -> navigateOnce(true)));
            return;
        }

        SettingsRepository settingsRepository = new SettingsRepository(this);
        settingsRepository.getLanguage("Bearer " + accessToken, new SettingsRepository.LanguageFetchCallback() {
            @Override
            public void onSuccess(String language) {
                AppLanguageManager.applyAppLanguage(language);
                ThemeSyncManager.syncThemeIfAuthenticated(SplashActivity.this,
                        () -> AppLockSyncManager.syncAppLockIfAuthenticated(SplashActivity.this,
                                () -> navigateOnce(true)));
            }

            @Override
            public void onError(String message) {
                ThemeSyncManager.syncThemeIfAuthenticated(SplashActivity.this,
                        () -> AppLockSyncManager.syncAppLockIfAuthenticated(SplashActivity.this,
                                () -> navigateOnce(true)));
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void scheduleNavigation(boolean isLoggedIn) {
        handler.postDelayed(() -> navigateOnce(isLoggedIn), SPLASH_DELAY_MILLIS);
    }

    private void navigateOnce(boolean isLoggedIn) {
        if (hasNavigated) {
            return;
        }

        hasNavigated = true;
        if (isLoggedIn) {
            navigateToMain();
        } else {
            navigateToLogin();
        }
    }
}
