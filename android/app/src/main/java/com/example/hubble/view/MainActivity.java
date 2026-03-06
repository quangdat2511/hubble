package com.example.hubble.view;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.ActivityMainBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseAuthActivity {

    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); } // No progress bar on main

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository()))
                .get(AuthViewModel.class);

        FirebaseUser user = authViewModel.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = user.getEmail() != null ? user.getEmail() : "User";
        }

        binding.tvWelcome.setText(getString(R.string.main_welcome, displayName));

        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            binding.tvEmail.setText(getString(R.string.main_email_label, email));
        } else if (user.getPhoneNumber() != null) {
            binding.tvEmail.setText(user.getPhoneNumber());
        }

        binding.btnLogout.setOnClickListener(v -> {
            authViewModel.logout();
            navigateToLogin();
        });
    }
}