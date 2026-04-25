package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.ActivityCreateChannelBinding;
import com.example.hubble.viewmodel.server.CreateChannelViewModel;
import com.example.hubble.viewmodel.server.CreateChannelViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class CreateChannelActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_PARENT_ID = "parent_id";
    private ActivityCreateChannelBinding binding;
    private CreateChannelViewModel viewModel;

    private final ActivityResultLauncher<Intent> accessLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Channel was created from the access screen
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            });

    public static Intent createIntent(Context context, String serverId) {
        Intent intent = new Intent(context, CreateChannelActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        return intent;
    }

    public static Intent createIntent(Context context, String serverId, String parentId) {
        Intent intent = new Intent(context, CreateChannelActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_PARENT_ID, parentId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityCreateChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        if (serverId == null) { finish(); return; }

        String parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);

        viewModel = new ViewModelProvider(this,
                new CreateChannelViewModelFactory(new ServerRepository(this), serverId))
                .get(CreateChannelViewModel.class);
        if (parentId != null) viewModel.setParentId(parentId);

        setupToolbar();
        setupChannelName();
        setupChannelType();
        setupPrivateToggle();
        observeCreateState();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAction.setOnClickListener(v -> onActionClick());
    }

    private void setupChannelName() {
        binding.etChannelName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setChannelName(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupChannelType() {
        // RadioGroup can't manage RadioButtons nested inside LinearLayouts,
        // so handle mutual exclusion manually
        binding.rbText.setOnClickListener(v -> {
            binding.rbText.setChecked(true);
            binding.rbVoice.setChecked(false);
            viewModel.setChannelType("TEXT");
        });
        binding.rbVoice.setOnClickListener(v -> {
            binding.rbVoice.setChecked(true);
            binding.rbText.setChecked(false);
            viewModel.setChannelType("VOICE");
        });
    }

    private void applyEdgeToEdge() {
        final int origTop = binding.getRoot().getPaddingTop();
        final int origBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), origTop + bars.top,
                    v.getPaddingRight(), origBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupPrivateToggle() {
        binding.switchPrivate.setOnCheckedChangeListener((btn, checked) -> {
            viewModel.setIsPrivate(checked);
            updateActionButton(checked);
        });
    }

    private void updateActionButton(boolean isPrivate) {
        binding.tvAction.setText(isPrivate
                ? R.string.create_channel_next
                : R.string.create_channel_create);
    }

    private void onActionClick() {
        String name = binding.etChannelName.getText() != null
                ? binding.etChannelName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.channel_name_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Boolean isPrivate = viewModel.isPrivate.getValue();

        if (Boolean.TRUE.equals(isPrivate)) {
            // Navigate to access (members/roles) screen
            String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
            String type = viewModel.channelType.getValue();
            String pid = viewModel.getParentId();
            Intent intent = CreateChannelAccessActivity.createIntent(this, serverId, name, type, pid);
            accessLauncher.launch(intent);
        } else {
            // Create channel directly
            viewModel.createChannel();
        }
    }

    private void observeCreateState() {
        viewModel.createState.observe(this, result -> {
            if (result == null) return;
            if (result.getStatus() == AuthResult.Status.LOADING) {
                binding.tvAction.setEnabled(false);
                return;
            }
            binding.tvAction.setEnabled(true);
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                String msg = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                viewModel.resetCreateState();
            }
        });
    }
}
