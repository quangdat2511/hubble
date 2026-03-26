package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityLanguageSettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;

public class LanguageSettingsActivity extends BaseAuthActivity {

    private ActivityLanguageSettingsBinding binding;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanguageSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(R.string.language_title);
        binding.toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
    }
}
