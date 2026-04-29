package com.example.hubble.view.friend;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.hubble.R;
import com.example.hubble.adapter.friend.FriendListAdapter;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityFriendsBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {
    private ActivityFriendsBinding binding;
    private FriendViewModel viewModel;
    private FriendListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
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

        adapter = new FriendListAdapter(this::confirmUnfriend);
        binding.rvFriends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFriends.setAdapter(adapter);

        observeViewModel();
        viewModel.fetchFriends();
    }

    private void confirmUnfriend(FriendUserDto user) {
        String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : user.getUsername();
        new AlertDialog.Builder(this)
                .setTitle(R.string.friend_unfriend_confirm_title)
                .setMessage(getString(R.string.friend_unfriend_confirm_message, displayName))
                .setPositiveButton(R.string.friend_unfriend, (dialog, which) ->
                        viewModel.unfriend(user.getId()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void observeViewModel() {
        viewModel.friendsList.observe(this, result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (result.isSuccess()) {
                List<FriendUserDto> data = result.getData();
                adapter.setFriends(data);
                binding.tvEmpty.setVisibility(
                        (data == null || data.isEmpty()) ? View.VISIBLE : View.GONE);
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
                    Snackbar.make(binding.getRoot(), R.string.friend_unfriend_success, Snackbar.LENGTH_SHORT).show();
                    viewModel.fetchFriends();
                } else if (result.isError()) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                viewModel.resetActionState();
            }
        });
    }
}
