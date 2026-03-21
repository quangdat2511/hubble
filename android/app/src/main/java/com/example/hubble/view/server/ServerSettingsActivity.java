package com.example.hubble.view.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityServerSettingsBinding;

public class ServerSettingsActivity extends AppCompatActivity {
    public static final String EXTRA_SERVER_ID = "extra_server_id";
    public static final String EXTRA_SERVER_NAME = "extra_server_name";

    private ActivityServerSettingsBinding binding;
    private String serverId;
    private String serverName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityServerSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        serverId = intent.getStringExtra(EXTRA_SERVER_ID);
        serverName = intent.getStringExtra(EXTRA_SERVER_NAME);

        if (savedInstanceState == null) {
            navigateTo(ServerSettingsFragment.newInstance(serverId, serverName), false);
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

    public String getServerId() {
        return serverId;
    }

    public static Intent createIntent(Context context, String serverId, String serverName) {
        Intent intent = new Intent(context, ServerSettingsActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_SERVER_NAME, serverName);
        return intent;
    }
}
