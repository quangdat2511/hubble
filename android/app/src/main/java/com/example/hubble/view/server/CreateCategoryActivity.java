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
import com.example.hubble.databinding.ActivityCreateCategoryBinding;
import com.example.hubble.viewmodel.server.CreateChannelViewModel;
import com.example.hubble.viewmodel.server.CreateChannelViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class CreateCategoryActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private ActivityCreateCategoryBinding binding;
    private CreateChannelViewModel viewModel;

    private final ActivityResultLauncher<Intent> accessLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            });

    public static Intent createIntent(Context context, String serverId) {
        Intent intent = new Intent(context, CreateCategoryActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityCreateCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        if (serverId == null) { finish(); return; }

        viewModel = new ViewModelProvider(this,
                new CreateChannelViewModelFactory(new ServerRepository(this), serverId))
                .get(CreateChannelViewModel.class);

        viewModel.setChannelType("CATEGORY");
        viewModel.setChannelName(getString(R.string.create_category_default_name));

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAction.setOnClickListener(v -> onActionClick());

        binding.etCategoryName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setChannelName(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.switchPrivate.setOnCheckedChangeListener((btn, checked) -> {
            viewModel.setIsPrivate(checked);
            binding.tvAction.setText(checked
                    ? R.string.create_channel_next
                    : R.string.create_channel_create);
        });

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

    private void onActionClick() {
        String name = binding.etCategoryName.getText() != null
                ? binding.etCategoryName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.category_name_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (Boolean.TRUE.equals(viewModel.isPrivate.getValue())) {
            String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
            Intent intent = CreateChannelAccessActivity.createIntent(this, serverId, name, "CATEGORY");
            accessLauncher.launch(intent);
        } else {
            viewModel.createChannel();
        }
    }
}
