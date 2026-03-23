package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivitySettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.SettingsViewModel;
import com.example.hubble.viewmodel.SettingsViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends BaseAuthActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

//        viewModel = new ViewModelProvider(this,
//                new SettingsViewModelFactory(new AuthRepository(this)))
//                .get(SettingsViewModel.class);


    }




}
