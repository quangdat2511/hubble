package com.example.hubble.view.friend;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.R;
import com.example.hubble.adapter.friend.OutgoingRequestAdapter;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityOutgoingRequestsBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class OutgoingRequestsActivity extends AppCompatActivity {
    private ActivityOutgoingRequestsBinding binding;
    private FriendViewModel viewModel;

    private OutgoingRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOutgoingRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new FriendViewModelFactory(new FriendRepository(this))).get(FriendViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new OutgoingRequestAdapter(request -> {
            viewModel.cancelOutgoingRequest(request.getId());
        });

        binding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRequests.setAdapter(adapter);

        observeViewModel();
        viewModel.fetchOutgoingRequests();
    }

    private void observeViewModel() {
        viewModel.outgoingRequests.observe(this, result -> {
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
                    Snackbar.make(binding.getRoot(), R.string.outgoing_request_cancelled, Snackbar.LENGTH_SHORT).show();
                    viewModel.fetchOutgoingRequests();
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                viewModel.resetActionState();
            }
        });
    }
}
