package com.example.hubble;

import android.app.Application;

import com.example.hubble.security.AppSwitcherProtectionManager;
import com.example.hubble.utils.ThemeManager;

public class HubbleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyStoredTheme(this);
        AppSwitcherProtectionManager.initialize(this);
    }
}
