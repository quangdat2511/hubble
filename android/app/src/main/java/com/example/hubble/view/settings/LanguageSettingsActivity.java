package com.example.hubble.view.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityLanguageSettingsBinding;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.view.base.BaseAuthActivity;

public class LanguageSettingsActivity extends BaseAuthActivity {

    private static final String EXTRA_SHOW_LANGUAGE_SAVED_MESSAGE = "extra_show_language_saved_message";

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

        if (getIntent().getBooleanExtra(EXTRA_SHOW_LANGUAGE_SAVED_MESSAGE, false)) {
            binding.getRoot().post(() -> {
                InAppMessageUtils.show(binding.getRoot(), getString(R.string.settings_saved));
                getIntent().removeExtra(EXTRA_SHOW_LANGUAGE_SAVED_MESSAGE);
            });
        }
    }

    public void relaunchForLocaleChange(boolean showSavedMessage) {
        Intent intent = new Intent(this, LanguageSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.putExtras(getIntent());
        intent.putExtra(EXTRA_SHOW_LANGUAGE_SAVED_MESSAGE, showSavedMessage);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
