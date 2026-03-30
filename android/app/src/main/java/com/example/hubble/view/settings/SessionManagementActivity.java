package com.example.hubble.view.settings;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.adapter.settings.SessionAdapter;
import com.example.hubble.data.repository.SessionRepository;
import com.example.hubble.databinding.ActivitySessionManagementBinding;
import com.example.hubble.viewmodel.SessionViewModel;
import com.example.hubble.viewmodel.SessionViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SessionManagementActivity extends AppCompatActivity {

    private ActivitySessionManagementBinding binding;
    private SessionViewModel viewModel;
    private SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySessionManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionRepository repository = new SessionRepository(this);
        viewModel = new ViewModelProvider(this, new SessionViewModelFactory(repository)).get(SessionViewModel.class);

        setupUI();
        observeViewModel();

        viewModel.fetchSessions();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new SessionAdapter(session -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Đăng xuất thiết bị")
                    .setMessage("Bạn có chắc muốn đăng xuất khỏi thiết bị " + session.getDeviceName() + "?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        viewModel.revokeSession(session.getId());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSessions.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.sessions.observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                if (result.isSuccess()) {
                    adapter.setSessions(result.getData());
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.revokeState.observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                if (result.isSuccess()) {
                    Snackbar.make(binding.getRoot(), "Đã đăng xuất thiết bị thành công", Snackbar.LENGTH_SHORT).show();
                    viewModel.fetchSessions();
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                viewModel.resetRevokeState();
            }
        });
    }
}