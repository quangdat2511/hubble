package com.example.hubble.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.databinding.ActivitySplashBinding;
import com.example.hubble.view.MainActivity;
import com.example.hubble.viewmodel.AuthViewModel;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (authViewModel.getCurrentUser() != null) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 1800);
    }
}
