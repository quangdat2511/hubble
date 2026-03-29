package com.example.hubble.view.dm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.dm.NewMessageAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.NewMessageItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityNewMessageBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewMessageActivity extends AppCompatActivity {

    private ActivityNewMessageBinding binding;
    private NewMessageAdapter adapter;
    private final List<NewMessageItem.Friend> allFriends = new ArrayList<>();
    private DmRepository dmRepository;

    public static Intent createIntent(Context context) {
        return new Intent(context, NewMessageActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        dmRepository = new DmRepository(this);

        setupHeader();
        setupQuickActions();
        setupList();
        loadFriendsFromBackend();
        setupSearch();
    }

    private void setupHeader() {
        binding.btnClose.setOnClickListener(v -> finish());
    }

    private void setupQuickActions() {
        binding.rowNewGroup.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.rowAddFriend.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
    }

    private void setupList() {
        adapter = new NewMessageAdapter();
        binding.rvFriends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFriends.setAdapter(adapter);

        adapter.setOnFriendClickListener(friend ->
                dmRepository.getOrCreateDirectChannel(friend.getId(), result -> {
                    if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                        dmRepository.rememberOpenedDirectChannel(result.getData().getId());
                        startActivity(DmChatActivity.createIntent(
                                this,
                                result.getData().getId(),
                                friend.getDisplayName()
                        ));
                        return;
                    }
                    String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                    Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
                }));
    }

    private void loadFriendsFromBackend() {
        dmRepository.getFriends(result -> {
            if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
                binding.tvEmptyFriends.setVisibility(android.view.View.VISIBLE);
                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
                return;
            }

            bindFriends(result.getData());
        });
    }

    private void bindFriends(List<FriendUserDto> friends) {
        allFriends.clear();
        for (FriendUserDto friend : friends) {
            String displayName = friend.getDisplayName() != null && !friend.getDisplayName().trim().isEmpty()
                    ? friend.getDisplayName()
                    : friend.getUsername();
            String username = friend.getUsername() != null ? friend.getUsername() : "";

            allFriends.add(new NewMessageItem.Friend(
                    friend.getId(),
                    displayName,
                    username,
                    null,
                    "ONLINE".equalsIgnoreCase(friend.getStatus())
            ));
        }

        binding.tvEmptyFriends.setVisibility(allFriends.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        renderList(allFriends);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }

    private void filterFriends(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            renderList(allFriends);
            return;
        }

        List<NewMessageItem.Friend> filtered = new ArrayList<>();
        for (NewMessageItem.Friend friend : allFriends) {
            String name = friend.getDisplayName().toLowerCase(Locale.ROOT);
            String username = friend.getUsername().toLowerCase(Locale.ROOT);
            if (name.contains(normalized) || username.contains(normalized)) {
                filtered.add(friend);
            }
        }
        renderList(filtered);
    }

    private void renderList(List<NewMessageItem.Friend> friends) {
        List<NewMessageItem> items = new ArrayList<>();
        if (friends.isEmpty()) {
            adapter.setItems(items);
            return;
        }

        items.add(new NewMessageItem.Section(getString(R.string.new_message_suggested)));
        int suggestedCount = Math.min(1, friends.size());
        for (int i = 0; i < suggestedCount; i++) {
            items.add(friends.get(i));
        }

        char currentGroup = 0;
        for (int i = suggestedCount; i < friends.size(); i++) {
            NewMessageItem.Friend friend = friends.get(i);
            char group = '#';
            if (friend.getDisplayName() != null && !friend.getDisplayName().trim().isEmpty()) {
                group = Character.toUpperCase(friend.getDisplayName().charAt(0));
            }
            if (group != currentGroup) {
                currentGroup = group;
                items.add(new NewMessageItem.Section(String.valueOf(currentGroup)));
            }
            items.add(friend);
        }

        adapter.setItems(items);
    }
}


