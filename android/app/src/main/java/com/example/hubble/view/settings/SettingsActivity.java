package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.PushConfigRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;

public class SettingsActivity extends BaseAuthActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.settingsLoadingIndicator; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new SettingsViewModelFactory(
                        new AuthRepository(this),
                        new PushConfigRepository(this)))
                .get(SettingsViewModel.class);

        setupToolbar();

        if (savedInstanceState == null) {
            navigateTo(new PushConfigFragment(), false);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        });
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void updateTitle(@StringRes int titleRes) {
        binding.toolbar.setTitle(titleRes);
    }

    public void setScreenLoading(boolean isLoading) {
        setLoadingState(isLoading);
    }

    public void logoutAndNavigateToLogin() {
        viewModel.logout();
        navigateToLogin();
    }
}
