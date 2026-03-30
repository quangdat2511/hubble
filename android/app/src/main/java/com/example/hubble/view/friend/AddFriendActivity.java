package com.example.hubble.view.friend;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.R;
import com.example.hubble.adapter.friend.FriendSearchAdapter;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityAddFriendBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class AddFriendActivity extends AppCompatActivity {
    private ActivityAddFriendBinding binding;
    private FriendViewModel viewModel;
    private FriendSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFriendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new FriendViewModelFactory(new FriendRepository(this))).get(FriendViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new FriendSearchAdapter(user -> viewModel.sendRequest(user.getUsername()));
        binding.rvResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResults.setAdapter(adapter);

        binding.btnSearch.setOnClickListener(v -> {
            String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                viewModel.searchUsers(query);
            }
        });

        viewModel.searchResults.observe(this, result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (result.isSuccess() && result.getData() != null) {
                adapter.setUsers(result.getData());
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.sendRequestState.observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(), R.string.friend_request_sent, Snackbar.LENGTH_SHORT).show();
                String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) viewModel.searchUsers(query);
                viewModel.resetSendState();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.resetSendState();
            }
        });
    }
}
