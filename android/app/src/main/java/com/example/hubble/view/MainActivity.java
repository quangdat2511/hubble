package com.example.hubble.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.NotificationRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.data.ws.ServerEventWebSocketManager;
import com.example.hubble.databinding.ActivityMainBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.base.BaseAuthActivity;
import com.example.hubble.view.home.HomeFragment;
import com.example.hubble.view.me.MeFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends BaseAuthActivity {

    private static final long HEARTBEAT_INTERVAL_MS = 60_000;

    private ActivityMainBinding binding;
    private String authToken;
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            sendHeartbeat();
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                    android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission granted");
                } else {
                    android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission denied");
                }
            });

    @Override
    protected View getRootView() { return binding.getRoot(); }

    @Override
    protected View getProgressBar() { return binding.getRoot(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must be called before setContentView to enable edge-to-edge on API 35+
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apply system bar insets so nothing is hidden under status / nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Fragment container: add top padding = status bar height
            binding.fragmentContainer.setPadding(0, bars.top, 0, 0);
            // Bottom nav: add bottom padding = navigation bar height
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        AuthViewModel authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(new AuthRepository(this)))
                .get(AuthViewModel.class);

        // Guard: redirect to login if not authenticated
        if (authViewModel.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission");
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Connect server-event WebSocket for real-time updates (kick, etc.)
        TokenManager tokenManager = new TokenManager(this);
        if (tokenManager.getUser() != null) {
            authToken = "Bearer " + tokenManager.getAccessToken();
            ServerEventWebSocketManager.getInstance().connect(
                    NetworkConfig.getApiBaseUrl(),
                    tokenManager.getUser().getId(),
                    tokenManager.getAccessToken()
            );

            // Signal online status and start periodic heartbeat
            callGoOnline();
            heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);

            NotificationRepository notificationRepo = new NotificationRepository(this);
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(notificationRepo::registerDeviceToken);
        }

        // Pre-create MainViewModel so HomeFragment can share it
        MainViewModel mainViewModel = new ViewModelProvider(this,
            new MainViewModelFactory(this, new DmRepository(this), new ServerRepository(this)))
            .get(MainViewModel.class);

        mainViewModel.dmTotalUnread.observe(this, count -> {
            int total = count != null ? count : 0;
            if (total <= 0) {
                // Drop the badge entirely when there is nothing to show so it
                // doesn't linger as a faint dot when the count cycles 1 → 0.
                binding.bottomNav.removeBadge(R.id.nav_home);
                return;
            }
            BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.nav_home);
            badge.setVisible(true);
            // Discord renders 99+ for 100 or more; Material handles this when
            // the displayed value would exceed maxCharacterCount.
            badge.setMaxCharacterCount(3);
            badge.setNumber(total);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.discord_nav_home_badge_blue));
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
        });

        setupBottomNavigation();

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            // Check if navigating from notification
            String navigateTo = getIntent().getStringExtra("navigateTo");
            if ("notifications".equals(navigateTo)) {
                switchFragment(new NotificationsFragment());
                binding.bottomNav.setSelectedItemId(R.id.nav_notifications);
            } else {
                switchFragment(new HomeFragment());
                binding.bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────

    private void setupBottomNavigation() {

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
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh server list when returning from ServerSettingsActivity
        // (e.g. after icon update/delete) — MainViewModel is scoped to this activity
        MainViewModel mainViewModel = new ViewModelProvider(this,
                new MainViewModelFactory(this, new DmRepository(this), new ServerRepository(this)))
                .get(MainViewModel.class);
        mainViewModel.refreshServers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        // Disconnect WebSocket when the app is fully closed
        if (isFinishing()) {
            callGoOffline();
            ServerEventWebSocketManager.getInstance().disconnect();
        }
    }

    // ── Status heartbeat ─────────────────────────────────────────────────

    private void sendHeartbeat() {
        if (authToken == null) return;
        RetrofitClient.getApiService(this).heartbeat(authToken)
                .enqueue(new retrofit2.Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<Void>> call,
                                           retrofit2.Response<ApiResponse<Void>> response) {
                        // no-op
                    }
                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<Void>> call, Throwable t) {
                        // no-op, will retry next interval
                    }
                });
    }

    private void callGoOnline() {
        if (authToken == null) return;
        RetrofitClient.getApiService(this).goOnline(authToken)
                .enqueue(new retrofit2.Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<Void>> call,
                                           retrofit2.Response<ApiResponse<Void>> response) {}
                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<Void>> call, Throwable t) {}
                });
    }

    private void callGoOffline() {
        if (authToken == null) return;
        try {
            // Synchronous call since we're in onDestroy
            RetrofitClient.getApiService(this).goOffline(authToken).execute();
        } catch (Exception ignored) {}
    }
}
