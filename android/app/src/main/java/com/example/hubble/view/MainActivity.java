package com.example.hubble.view;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.ActivityMainBinding;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.view.home.HomeFragment;
import com.example.hubble.view.me.MeFragment;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends BaseAuthActivity {

    private ActivityMainBinding binding;

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthViewModel authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        // Guard: redirect to login if not authenticated
        if (authViewModel.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        // Pre-create MainViewModel so HomeFragment can share it
        new ViewModelProvider(this,
            new MainViewModelFactory(new DmRepository(this), new ServerRepository(this)))
            .get(MainViewModel.class);

        setupBottomNavigation();

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────

    private void setupBottomNavigation() {
        // Badge on Home tab (unread count stub)
        BadgeDrawable homeBadge = binding.bottomNav.getOrCreateBadge(R.id.nav_home);
        homeBadge.setNumber(175);
        homeBadge.setVisible(true);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_notifications) {
                switchFragment(new NotificationsFragment());
                return true;
            } else if (id == R.id.nav_me) {
                switchFragment(new MeFragment());
                return true;
            }
            return false;
        });
    }

    // ── Fragment Switching ────────────────────────────────────────────────

    private void switchFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment);
        tx.commit();
    }
}