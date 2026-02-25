package com.example.hubble.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityMainBinding;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

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
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}