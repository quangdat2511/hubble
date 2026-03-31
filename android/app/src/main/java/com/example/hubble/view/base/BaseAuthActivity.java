package com.example.hubble.view.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.view.MainActivity;
import com.example.hubble.view.auth.LoginActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Base Activity for all auth-related screens.
 * Consolidates duplicated navigation, error display, loading state, and observer logic.
 */
public abstract class BaseAuthActivity extends AppCompatActivity {

    protected abstract View getRootView();
    protected abstract View getProgressBar();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Opt-in to edge-to-edge before the window is laid out
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
    }

    // ─── Edge-to-Edge helper ─────────────────────────────────────

    /**
     * Call this AFTER setContentView(). Applies status-bar top padding and
     * navigation-bar bottom padding to the given root view, preserving any
     * padding already declared in the layout XML.
     */
    protected void applyEdgeToEdge(View rootView) {
        // Snapshot padding set in XML before any inset listener fires
        final int origLeft   = rootView.getPaddingLeft();
        final int origTop    = rootView.getPaddingTop();
        final int origRight  = rootView.getPaddingRight();
        final int origBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    origLeft   + bars.left,
                    origTop    + bars.top,
                    origRight  + bars.right,
                    origBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ─── Navigation ──────────────────────────────────────────────

    protected void navigateToMain() {
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

    // ─── UI Helpers ──────────────────────────────────────────────

    protected void showError(String message) {
        Snackbar.make(getRootView(),
                message != null ? message : getString(R.string.error_generic),
                Snackbar.LENGTH_LONG).show();
    }

    protected void setLoadingState(boolean isLoading) {
        getProgressBar().setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // ─── LiveData Observer Helper ────────────────────────────────

    /**
     * Standard observer for AuthResult LiveData:
     * - LOADING  → show progress
     * - SUCCESS  → hide progress, reset state, run onSuccess
     * - ERROR    → hide progress, reset state, show error message
     */
    protected <T> void observeAuthResult(
            LiveData<AuthResult<T>> liveData,
            Runnable resetState,
            Runnable onSuccess) {
        observeAuthResult(liveData, resetState, onSuccess, this::showError);
    }

    /** Observer with custom error handler. */
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
}
