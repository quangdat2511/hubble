package com.example.hubble.view.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.adapter.search.SearchChannelAdapter;
import com.example.hubble.adapter.search.SearchFileAdapter;
import com.example.hubble.adapter.search.SearchMediaAdapter;
import com.example.hubble.adapter.search.SearchMemberAdapter;
import com.example.hubble.adapter.search.SearchMessageAdapter;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.search.SearchAttachmentDto;
import com.example.hubble.data.model.search.SearchChannelDto;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.data.model.search.SearchMessageDto;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.FragmentSearchResultsBinding;
import com.example.hubble.view.dm.DmChatActivity;
import com.example.hubble.view.dm.ImageViewerActivity;
import com.example.hubble.view.server.MemberDetailBottomSheet;
import com.example.hubble.viewmodel.SearchViewModel;
import com.example.hubble.viewmodel.SearchViewModel.ScopeType;

import java.util.List;

/**
 * One tab in SearchActivity. Category is passed via a Bundle arg (KEY_CATEGORY).
 */
public class SearchResultFragment extends Fragment {

    public enum Category {
        MESSAGES, MEMBERS, CHANNELS, MEDIA, FILES, PINS, FRIENDS
    }

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_SCOPE_TYPE = "scope_type";
    private static final String KEY_SCOPE_ID = "scope_id";

    public static SearchResultFragment newInstance(Category category, ScopeType scopeType,
                                                   @Nullable String scopeId) {
        SearchResultFragment f = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(KEY_CATEGORY, category.name());
        args.putString(KEY_SCOPE_TYPE, scopeType.name());
        args.putString(KEY_SCOPE_ID, scopeId);
        f.setArguments(args);
        return f;
    }

    private FragmentSearchResultsBinding binding;
    private SearchViewModel viewModel;
    private Category category;
    private ScopeType scopeType;
    @Nullable private String scopeId;

    // Adapters
    private SearchMessageAdapter messageAdapter;
    private SearchMemberAdapter memberAdapter;
    private SearchChannelAdapter channelAdapter;
    private SearchMediaAdapter mediaAdapter;
    private SearchFileAdapter fileAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            category = Category.valueOf(args.getString(KEY_CATEGORY, Category.MESSAGES.name()));
            scopeType = ScopeType.valueOf(args.getString(KEY_SCOPE_TYPE, ScopeType.CHANNEL.name()));
            scopeId = args.getString(KEY_SCOPE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        switch (category) {
            case MESSAGES:
            case PINS:
                messageAdapter = new SearchMessageAdapter();
                messageAdapter.setOnItemClickListener(this::onMessageClick);
                binding.rvResults.setLayoutManager(
                        new LinearLayoutManager(requireContext()));
                binding.rvResults.setAdapter(messageAdapter);
                if (category == Category.MESSAGES) {
                    binding.rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                            LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                            if (lm == null) return;
                            int last = lm.findLastVisibleItemPosition();
                            if (last >= messageAdapter.getItemCount() - 3
                                    && !viewModel.isMessagesLastPage()) {
                                viewModel.loadMoreMessages();
                            }
                        }
                    });
                }
                break;

            case MEMBERS:
            case FRIENDS:
                memberAdapter = new SearchMemberAdapter();
                memberAdapter.setOnItemClickListener(this::onMemberClick);
                binding.rvResults.setLayoutManager(
                        new LinearLayoutManager(requireContext()));
                binding.rvResults.setAdapter(memberAdapter);
                break;

            case CHANNELS:
                channelAdapter = new SearchChannelAdapter();
                channelAdapter.setOnItemClickListener(this::onChannelClick);
                binding.rvResults.setLayoutManager(
                        new LinearLayoutManager(requireContext()));
                binding.rvResults.setAdapter(channelAdapter);
                break;

            case MEDIA:
                mediaAdapter = new SearchMediaAdapter();
                mediaAdapter.setOnItemClickListener(this::onMediaClick);
                binding.rvResults.setLayoutManager(new GridLayoutManager(requireContext(), 3));
                binding.rvResults.setAdapter(mediaAdapter);
                break;

            case FILES:
                fileAdapter = new SearchFileAdapter();
                fileAdapter.setOnItemClickListener(this::onFileClick);
                binding.rvResults.setLayoutManager(
                        new LinearLayoutManager(requireContext()));
                binding.rvResults.setAdapter(fileAdapter);
                break;
        }
    }

    private void observeViewModel() {
        switch (category) {
            case MESSAGES:
                viewModel.messagesResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchMessageDto> data = result.getData();
                        messageAdapter.setHighlightQuery(
                                viewModel.currentQuery.getValue());
                        messageAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
            case PINS:
                viewModel.pinsResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchMessageDto> data = result.getData();
                        messageAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
            case MEMBERS:
            case FRIENDS:
                viewModel.membersResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchMemberDto> data = result.getData();
                        memberAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
            case CHANNELS:
                viewModel.channelsResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchChannelDto> data = result.getData();
                        channelAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
            case MEDIA:
                viewModel.mediaResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchAttachmentDto> data = result.getData();
                        mediaAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
            case FILES:
                viewModel.filesResult.observe(getViewLifecycleOwner(), result -> {
                    if (result == null) return;
                    if (result.isSuccess()) {
                        List<SearchAttachmentDto> data = result.getData();
                        fileAdapter.setItems(data);
                        setEmpty(data == null || data.isEmpty());
                    } else if (result.isError()) {
                        setEmpty(true);
                    }
                });
                break;
        }
    }

    private void setEmpty(boolean empty) {
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvResults.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Click handlers ────────────────────────────────────────────────────

    private void onMessageClick(SearchMessageDto item) {
        if (item.getChannelId() == null) return;
        String query = viewModel.currentQuery.getValue();
        Intent intent;
        if (scopeType == ScopeType.SERVER && scopeId != null) {
            intent = DmChatActivity.createIntentForServerText(
                    requireContext(), scopeId, null, null, null,
                    item.getChannelId(), item.getChannelName() != null ? "#" + item.getChannelName() : "",
                    null, null, null, false);
        } else {
            intent = DmChatActivity.createIntent(requireContext(), item.getChannelId(), null);
        }
        intent.putExtra(DmChatActivity.EXTRA_JUMP_MESSAGE_ID, item.getId());
        if (query != null && !query.trim().isEmpty()) {
            intent.putExtra(DmChatActivity.EXTRA_HIGHLIGHT_QUERY, query.trim());
        }
        startActivity(intent);
    }

    private void onMemberClick(SearchMemberDto item) {
        if (scopeType == ScopeType.SERVER && scopeId != null) {
            ServerMemberItem memberItem = new ServerMemberItem(
                    item.getId(), item.getUsername(), item.getDisplayName(),
                    item.getAvatarUrl() != null ? NetworkConfig.resolveUrl(item.getAvatarUrl()) : null,
                    0, null, item.getStatus(), false);
            MemberDetailBottomSheet.newInstance(memberItem, scopeId)
                    .show(getChildFragmentManager(), "member_detail");
        } else {
            // DM or channel scope: open DM with this user (need channelId — not available here)
            // For DM scope: start new DM conversation. For channel: show profile.
            // We open a DM conversation; channelId unknown so let the backend create/find it.
            // For now show a placeholder.
            if (scopeType == ScopeType.DM && item.getId() != null) {
                // For DM scope, member is a friend — open DM conversation
                // (channel ID not known from member search; use NewMessageActivity flow)
                android.widget.Toast.makeText(requireContext(),
                        item.getDisplayName() != null ? item.getDisplayName() : item.getUsername(),
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onChannelClick(SearchChannelDto item) {
        if (item.getId() == null || scopeId == null) return;
        Intent intent = DmChatActivity.createIntentForServerText(
                requireContext(), scopeId, null, null, null,
                item.getId(), item.getName() != null ? "#" + item.getName() : "",
                item.getTopic(), null, null, false);
        startActivity(intent);
    }

    private void onMediaClick(SearchAttachmentDto item) {
        if (item.getUrl() == null) return;
        Intent intent = new Intent(requireContext(), ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, NetworkConfig.resolveUrl(item.getUrl()));
        intent.putExtra(ImageViewerActivity.EXTRA_FILE_NAME,
                item.getFilename() != null ? item.getFilename() : "");
        startActivity(intent);
    }

    private void onFileClick(SearchAttachmentDto item) {
        if (item.getMessageId() == null || item.getChannelId() == null) return;
        // Jump to the message containing this file
        Intent intent;
        if (scopeType == ScopeType.SERVER && scopeId != null) {
            intent = DmChatActivity.createIntentForServerText(
                    requireContext(), scopeId, null, null, null,
                    item.getChannelId(), "", null, null, null, false);
        } else {
            intent = DmChatActivity.createIntent(requireContext(), item.getChannelId(), null);
        }
        intent.putExtra(DmChatActivity.EXTRA_JUMP_MESSAGE_ID, item.getMessageId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
