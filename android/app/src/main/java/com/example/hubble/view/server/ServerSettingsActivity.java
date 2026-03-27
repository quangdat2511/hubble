package com.example.hubble.view.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityServerSettingsBinding;

public class ServerSettingsActivity extends AppCompatActivity {
    public static final String EXTRA_SERVER_ID   = "extra_server_id";
    public static final String EXTRA_SERVER_NAME = "extra_server_name";
    public static final String EXTRA_OWNER_ID    = "extra_owner_id";
    public static final String EXTRA_ICON_URL    = "extra_icon_url";
    public static final String EXTRA_OPEN_INVITES = "extra_open_invites";

    private ActivityServerSettingsBinding binding;
    private String serverId;
    private String serverName;
    private String ownerId;
    private String iconUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);

        binding = ActivityServerSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Push content below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.fragmentContainer.setPadding(0, bars.top, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        Intent intent = getIntent();
        serverId   = intent.getStringExtra(EXTRA_SERVER_ID);
        serverName = intent.getStringExtra(EXTRA_SERVER_NAME);
        ownerId    = intent.getStringExtra(EXTRA_OWNER_ID);
        iconUrl    = intent.getStringExtra(EXTRA_ICON_URL);

        if (savedInstanceState == null) {
            navigateTo(ServerSettingsFragment.newInstance(serverId, serverName, ownerId, iconUrl), false);
            if (intent.getBooleanExtra(EXTRA_OPEN_INVITES, false)) {
                navigateTo(ServerInviteFragment.newInstance(serverId, serverName), true);
            }
        }
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public String getServerId() { return serverId; }

    public static Intent createIntent(Context context, String serverId, String serverName,
                                      String ownerId, String iconUrl) {
        Intent intent = new Intent(context, ServerSettingsActivity.class);
        intent.putExtra(EXTRA_SERVER_ID,   serverId);
        intent.putExtra(EXTRA_SERVER_NAME, serverName);
        intent.putExtra(EXTRA_OWNER_ID,    ownerId);
        intent.putExtra(EXTRA_ICON_URL,    iconUrl);
        return intent;
    }

    /** Backwards-compatible overload used by callers without ownerId/iconUrl. */
    public static Intent createIntent(Context context, String serverId, String serverName) {
        return createIntent(context, serverId, serverName, null, null);
    }
}
