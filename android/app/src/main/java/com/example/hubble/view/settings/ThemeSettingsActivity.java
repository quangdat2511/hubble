package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import com.example.hubble.databinding.ActivityThemeSettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;

public class ThemeSettingsActivity extends BaseAuthActivity {

    private ActivityThemeSettingsBinding binding;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(binding.fragmentContainer.getId(), new ThemeFragment())
                    .commit();
        }
    }
}
