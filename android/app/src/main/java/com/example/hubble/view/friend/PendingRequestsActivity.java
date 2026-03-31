package com.example.hubble.view.friend;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.adapter.friend.FriendRequestAdapter;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityPendingRequestsBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class PendingRequestsActivity extends AppCompatActivity implements FriendRequestAdapter.OnRequestListener {
    private ActivityPendingRequestsBinding binding;
    private FriendViewModel viewModel;
    private FriendRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityPendingRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        viewModel = new ViewModelProvider(this,
                new FriendViewModelFactory(new FriendRepository(this))).get(FriendViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new FriendRequestAdapter(this);
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRequests.setAdapter(adapter);

        observeViewModel();
        viewModel.fetchIncomingRequests();
    }

    private void observeViewModel() {
        viewModel.incomingRequests.observe(this, result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (result.isSuccess() && result.getData() != null) {
                adapter.setRequests(result.getData());
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.actionState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                if (result.isSuccess()) {
                    Snackbar.make(binding.getRoot(), "Đã xử lý lời mời!", Snackbar.LENGTH_SHORT).show();
                    viewModel.fetchIncomingRequests();
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                viewModel.resetActionState();
            }
        });
    }

    @Override
    public void onAccept(FriendRequestResponse request) {
        viewModel.acceptRequest(request.getId());
    }

    @Override
    public void onDecline(FriendRequestResponse request) {
        viewModel.declineRequest(request.getId());
    }
}