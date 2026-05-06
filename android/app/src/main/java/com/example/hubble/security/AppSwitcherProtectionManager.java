package com.example.hubble.security;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class AppSwitcherProtectionManager implements Application.ActivityLifecycleCallbacks {

    private static AppSwitcherProtectionManager instance;

    private final AppSwitcherProtectionRepository repository;
    private final Set<Activity> trackedActivities =
            Collections.newSetFromMap(new WeakHashMap<>());

    private AppSwitcherProtectionManager(Application application) {
        repository = new AppSwitcherProtectionRepository(application);
    }

    public static synchronized void initialize(Application application) {
        if (instance != null) {
            return;
        }

        instance = new AppSwitcherProtectionManager(application);
        application.registerActivityLifecycleCallbacks(instance);
    }

    public static AppSwitcherProtectionManager getInstance() {
        return instance;
    }

    public void refreshProtection() {
        for (Activity activity : new ArrayList<>(trackedActivities)) {
            if (activity != null) {
                applyWindowProtection(activity);
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        trackedActivities.add(activity);
        applyWindowProtection(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        trackedActivities.add(activity);
        applyWindowProtection(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        trackedActivities.add(activity);
        applyWindowProtection(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        trackedActivities.remove(activity);
    }

    private void applyWindowProtection(Activity activity) {
        if (repository.isEnabled()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
