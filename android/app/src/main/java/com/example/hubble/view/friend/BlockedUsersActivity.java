package com.example.hubble.view.friend;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.adapter.friend.FriendSearchAdapter;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityAddFriendBinding;
import com.example.hubble.databinding.ActivityBlockedUsersBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class BlockedUsersActivity extends AppCompatActivity {
    private ActivityBlockedUsersBinding binding;
    private FriendViewModel viewModel;
    private FriendSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlockedUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this,
                new FriendViewModelFactory(new FriendRepository(this))).get(FriendViewModel.class);

        binding.toolbar.setTitle("Người dùng đã chặn");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

//        binding.etSearch.setVisibility(View.GONE);
//        binding.btnSearch.setVisibility(View.GONE);

        adapter = new FriendSearchAdapter(user -> viewModel.unblockUser(user.getId()));
        binding.rvResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResults.setAdapter(adapter);

        observeViewModel();
        viewModel.fetchBlockedUsers();
    }

    private void observeViewModel() {
        viewModel.blockedUsers.observe(this, result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (result.isSuccess() && result.getData() != null) {
                adapter.setUsers(result.getData());
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
                    Snackbar.make(binding.getRoot(), "Đã bỏ chặn thành công!", Snackbar.LENGTH_SHORT).show();
                    viewModel.fetchBlockedUsers();
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                viewModel.resetActionState();
            }
        });
    }
}