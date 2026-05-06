package com.example.hubble.security;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.ForgotPasswordActivity;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.auth.OtpActivity;
import com.example.hubble.view.auth.RegisterActivity;
import com.example.hubble.view.auth.SplashActivity;
import com.example.hubble.view.security.AppLockActivity;

import java.lang.ref.WeakReference;

public class AppLockManager implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private static AppLockManager instance;

    private final AppLockRepository repository;
    private WeakReference<Activity> currentActivity = new WeakReference<>(null);
    private String activeUserId;
    private boolean unlockRequired;
    private boolean promptVisible;

    private AppLockManager(Application application) {
        this.repository = new AppLockRepository(application);
    }

    public static synchronized void initialize(Application application) {
        if (instance != null) {
            return;
        }

        instance = new AppLockManager(application);
        application.registerActivityLifecycleCallbacks(instance);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(instance);
    }

    public static AppLockManager getInstance() {
        return instance;
    }

    public void onUnlockSucceeded() {
        promptVisible = false;
        unlockRequired = false;
    }

    public void onSessionEnded() {
        activeUserId = null;
        promptVisible = false;
        unlockRequired = false;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (repository.isPasscodeEnabled() && repository.hasStoredPin()) {
            unlockRequired = true;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = new WeakReference<>(activity);
        if (activity instanceof AppLockActivity) {
            promptVisible = true;
            return;
        }
        syncUserState();
        maybeShowLock(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity instanceof AppLockActivity && activity.isFinishing()) {
            promptVisible = false;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Activity current = currentActivity.get();
        if (current == activity) {
            currentActivity.clear();
        }
    }

    private void maybeShowLock(Activity activity) {
        if (promptVisible
                || !shouldProtect(activity)
                || !unlockRequired
                || !repository.isPasscodeEnabled()
                || !repository.hasStoredPin()) {
            return;
        }

        promptVisible = true;
        activity.startActivity(AppLockActivity.createIntent(activity));
        activity.overridePendingTransition(0, 0);
    }

    private boolean shouldProtect(Activity activity) {
        if (activity == null || new TokenManager(activity).getUser() == null) {
            return false;
        }
        return !(activity instanceof SplashActivity)
                && !(activity instanceof LoginActivity)
                && !(activity instanceof RegisterActivity)
                && !(activity instanceof OtpActivity)
                && !(activity instanceof ForgotPasswordActivity)
                && !(activity instanceof AppLockActivity);
    }

    private void syncUserState() {
        String currentUserId = repository.getCurrentUserId();
        if (currentUserId == null) {
            onSessionEnded();
            return;
        }

        if (currentUserId.equals(activeUserId)) {
            if (repository.isPasscodeEnabled() && !repository.hasStoredPin()) {
                repository.setPasscodeEnabled(false);
                unlockRequired = false;
            }
            return;
        }

        activeUserId = currentUserId;
        promptVisible = false;
        if (repository.isPasscodeEnabled() && repository.hasStoredPin()) {
            unlockRequired = true;
        } else {
            if (repository.isPasscodeEnabled()) {
                repository.setPasscodeEnabled(false);
            }
            unlockRequired = false;
        }
    }
}
