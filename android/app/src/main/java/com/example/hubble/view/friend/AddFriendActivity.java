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
import com.example.hubble.R;
import com.example.hubble.adapter.friend.FriendSearchAdapter;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityAddFriendBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class AddFriendActivity extends AppCompatActivity {
    private ActivityAddFriendBinding binding;
    private FriendViewModel viewModel;
    private FriendSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityAddFriendBinding.inflate(getLayoutInflater());
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

        adapter = new FriendSearchAdapter(
                // onAdd
                user -> viewModel.sendRequest(user.getUsername()),
                // onBlock — confirm dialog before blocking
                user -> {
                    String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                            ? user.getDisplayName() : user.getUsername();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.block_user_confirm_title)
                            .setMessage(getString(R.string.block_user_confirm_message, displayName))
                            .setPositiveButton(R.string.friend_action_block, (dialog, which) ->
                                    viewModel.blockUser(user.getId()))
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
        );
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
                refreshSearch();
                viewModel.resetSendState();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.resetSendState();
            }
        });

        viewModel.actionState.observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
                return;
            }
            binding.progressBar.setVisibility(View.GONE);
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(), R.string.blocked_user_blocked, Snackbar.LENGTH_SHORT).show();
                refreshSearch();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
            viewModel.resetActionState();
        });
    }

    private void refreshSearch() {
        String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
        if (!query.isEmpty()) viewModel.searchUsers(query);
    }
}
