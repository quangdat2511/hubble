package com.example.hubble.view.base;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.hubble.R;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.utils.ThemeManager;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.MainActivity;
import com.example.hubble.view.auth.LoginActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Base Activity for all auth-related screens.
 * Consolidates duplicated navigation, error display, loading state, and observer logic.
 */
public abstract class BaseAuthActivity extends AppCompatActivity {

    private static final long THEME_FETCH_TIMEOUT_MS = 4000L;

    private final Handler navigationHandler = new Handler(Looper.getMainLooper());

    private boolean navigatingToMain;
    private Runnable pendingMainNavigationTimeout;

    protected abstract View getRootView();
    protected abstract View getProgressBar();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyStoredTheme(this);
        super.onCreate(savedInstanceState);
    }

    protected void navigateToMain() {
        if (navigatingToMain) {
            return;
        }

        String accessToken = new TokenManager(this).getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            launchMainActivity();
            return;
        }

        navigatingToMain = true;
        SettingsRepository settingsRepository = new SettingsRepository(this);
        pendingMainNavigationTimeout = this::launchMainActivity;
        navigationHandler.postDelayed(pendingMainNavigationTimeout, THEME_FETCH_TIMEOUT_MS);

        settingsRepository.fetchTheme("Bearer " + accessToken, new SettingsRepository.ThemeFetchCallback() {
            @Override
            public void onSuccess(String theme) {
                ThemeManager.saveTheme(BaseAuthActivity.this, theme);
                launchMainActivity();
            }

            @Override
            public void onError(String message) {
                launchMainActivity();
            }
        });
    }

    private void launchMainActivity() {
        if (pendingMainNavigationTimeout != null) {
            navigationHandler.removeCallbacks(pendingMainNavigationTimeout);
            pendingMainNavigationTimeout = null;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    protected void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    protected void showError(String message) {
        Snackbar.make(getRootView(),
                message != null ? message : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }

    protected void setLoadingState(boolean isLoading) {
        getProgressBar().setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    protected <T> void observeAuthResult(
            LiveData<AuthResult<T>> liveData,
            Runnable resetState,
            Runnable onSuccess) {
        observeAuthResult(liveData, resetState, onSuccess, this::showError);
    }

    protected <T> void observeAuthResult(
            LiveData<AuthResult<T>> liveData,
            Runnable resetState,
            Runnable onSuccess,
            java.util.function.Consumer<String> onError) {
        liveData.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                setLoadingState(true);
            } else if (result.isSuccess()) {
                setLoadingState(false);
                resetState.run();
                onSuccess.run();
            } else {
                setLoadingState(false);
                resetState.run();
                onError.accept(result.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        navigationHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
