package com.example.hubble.view.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.hubble.databinding.ActivityLanguageSettingsBinding;
import com.example.hubble.view.base.BaseAuthActivity;

public class LanguageSettingsActivity extends BaseAuthActivity {

    private ActivityLanguageSettingsBinding binding;

    @Override
    protected View getRootView() {
        return binding.getRoot();
    }

    @Override
    protected View getProgressBar() {
        return binding.getRoot();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanguageSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    public void relaunchForLocaleChange() {
        Intent intent = new Intent(this, LanguageSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
