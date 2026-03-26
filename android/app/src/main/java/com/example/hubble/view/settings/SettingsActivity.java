package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.data.repository.SettingsRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class SettingsActivity extends BaseAuthActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.settingsLoadingIndicator; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new ViewModelProvider(
                this,
                new SettingsViewModelFactory(
                        new AuthRepository(this),
                        new SettingsRepository(this),
                        new PushConfigRepository(this))
        ).get(SettingsViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(binding.languageFragmentContainer.getId(), new LanguageFragment())
                    .replace(binding.pushConfigFragmentContainer.getId(), new PushConfigFragment())
                    .replace(binding.themeFragmentContainer.getId(), new ThemeFragment())
                    .commit();
        }
    }

    public void setScreenLoading(boolean isLoading) {
        setLoadingState(isLoading);
    }
}
