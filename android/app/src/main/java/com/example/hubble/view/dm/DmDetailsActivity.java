package com.example.hubble.view.dm;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmConversationOverviewAdapter;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.SharedContentItemResponse;
import com.example.hubble.data.model.dm.SharedContentPageResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmDetailsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DmDetailsActivity extends AppCompatActivity implements DmConversationOverviewAdapter.Listener {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    private static final String EXTRA_USERNAME = "extra_username";
    private static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    private static final int SHARED_CONTENT_PAGE_SIZE = 24;

    private ActivityDmDetailsBinding binding;
    private DmRepository dmRepository;
    private DmConversationOverviewAdapter adapter;
    private GridLayoutManager layoutManager;

    private String channelId;
    private String peerDisplayName;
    private String peerUsername;
    private String peerAvatarUrl;
    private DmDetailsTab currentTab = DmDetailsTab.MEDIA;
    private final Map<DmDetailsTab, TabState> tabStates = new EnumMap<>(DmDetailsTab.class);

    public static Intent createIntent(Context context, String channelId, String displayName,
                                      String username, String avatarUrl) {
        Intent intent = new Intent(context, DmDetailsActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_DISPLAY_NAME, displayName);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_AVATAR_URL, avatarUrl);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityDmDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dmRepository = new DmRepository(this);

        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        peerDisplayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        peerUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        peerAvatarUrl = getIntent().getStringExtra(EXTRA_AVATAR_URL);

        setupToolbar();
        setupHeader();
        setupTabs();
        setupRecyclerView();
        binding.btnRetry.setOnClickListener(v -> refreshContent(true));

        loadPeerProfile();
        refreshContent(true);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupHeader() {
        bindPeerHeader();
        binding.tvSharedSummary.setText(getString(
                R.string.dm_gallery_summary,
                firstNonBlank(peerDisplayName, getString(R.string.dm_default_user))
        ));
    }

    private void setupTabs() {
        for (DmDetailsTab tab : DmDetailsTab.values()) {
            binding.tabLayout.addTab(binding.tabLayout.newTab()
                    .setText(tab.getLabelResId())
                    .setTag(tab.getTag()));
            tabStates.put(tab, new TabState());
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = DmDetailsTab.fromTag(tab.getTag());
                bindPeerHeader();
                renderCurrentState();
                if (!getCurrentState().hasLoaded) {
                    refreshContent(true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                binding.rvSharedContent.smoothScrollToPosition(0);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new DmConversationOverviewAdapter(this);
        layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(adapter.createSpanSizeLookup());

        binding.rvSharedContent.setLayoutManager(layoutManager);
        binding.rvSharedContent.setAdapter(adapter);
        binding.rvSharedContent.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                TabState currentState = getCurrentState();
                if (dy <= 0 || currentState.isLoading || !currentState.hasMore) {
                    return;
                }
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 6) {
                    loadNextPage();
                }
            }
        });
    }

    private void loadPeerProfile() {
        if (TextUtils.isEmpty(channelId)) {
            return;
        }
        dmRepository.getDirectChannels(result -> {
            if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
                return;
            }
            for (ChannelDto channel : result.getData()) {
                if (channel != null && channelId.equals(channel.getId())) {
                    runOnUiThread(() -> {
                        peerDisplayName = firstNonBlank(channel.getPeerDisplayName(), peerDisplayName);
                        peerUsername = firstNonBlank(channel.getPeerUsername(), peerUsername);
                        peerAvatarUrl = firstNonBlank(channel.getPeerAvatarUrl(), peerAvatarUrl);
                        bindPeerHeader();
                    });
                    return;
                }
            }
        });
    }

    private void bindPeerHeader() {
        String displayName = firstNonBlank(peerDisplayName, getString(R.string.dm_default_user));
        String username = firstNonBlank(peerUsername, displayName);

        binding.tvHeaderName.setText(displayName);
        binding.tvHeaderSubtitle.setText(getString(R.string.dm_gallery_username_format, username));
        binding.tvSharedSummary.setText(getString(R.string.dm_gallery_summary, displayName));
        binding.tvToolbarTitle.setText(displayName);
        binding.tvToolbarSubtitle.setText(getString(
                R.string.dm_gallery_toolbar_subtitle,
                getString(currentTab.getLabelResId())
        ));

        Glide.with(this)
                .load(NetworkConfig.resolveUrl(peerAvatarUrl))
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(binding.ivHeaderAvatar);
    }

    private void refreshContent(boolean reset) {
        if (TextUtils.isEmpty(channelId)) {
            showErrorState(getString(R.string.dm_gallery_error_generic));
            return;
        }

        TabState state = getCurrentState();
        if (reset) {
            state.reset();
            adapter.submitOverviewItems(new ArrayList<>());
            binding.progressInitial.setVisibility(View.VISIBLE);
            binding.rvSharedContent.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.layoutErrorState.setVisibility(View.GONE);
        } else {
            binding.progressLoadMore.setVisibility(View.VISIBLE);
        }

        state.isLoading = true;
        state.errorMessage = null;

        int requestedPage = state.nextPage;
        dmRepository.getSharedContent(channelId, currentTab.getRequestType(), requestedPage, SHARED_CONTENT_PAGE_SIZE,
                result -> runOnUiThread(() -> handleSharedContentResult(result, reset, currentTab, requestedPage)));
    }

    private void loadNextPage() {
        TabState currentState = getCurrentState();
        if (currentState.isLoading || !currentState.hasMore) {
            return;
        }
        refreshContent(false);
    }

    private void handleSharedContentResult(AuthResult<SharedContentPageResponse> result, boolean reset,
                                           @NonNull DmDetailsTab tab, int requestedPage) {
        TabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        state.isLoading = false;
        binding.progressInitial.setVisibility(View.GONE);
        binding.progressLoadMore.setVisibility(View.GONE);

        if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
            SharedContentPageResponse page = result.getData();
            List<DmOverviewItem> mappedItems = new ArrayList<>();
            for (SharedContentItemResponse response : page.getItems()) {
                if (response != null) {
                    mappedItems.add(DmOverviewItem.fromSharedContent(this, response));
                }
            }
            if (reset) {
                state.items.clear();
            }
            state.items.addAll(mappedItems);
            state.nextPage = requestedPage + 1;
            state.hasMore = page.hasMore();
            state.hasLoaded = true;
            if (tab == currentTab) {
                renderCurrentState();
            }
            return;
        }

        handleLoadFailure(state, reset, result.getMessage());
    }
    private void handleLoadFailure(@NonNull TabState state, boolean reset, @Nullable String message) {
        state.isLoading = false;
        state.errorMessage = message != null ? message : getString(R.string.dm_gallery_error_generic);
        if (!reset && !state.items.isEmpty()) {
            Snackbar.make(binding.getRoot(), state.errorMessage, Snackbar.LENGTH_LONG).show();
            return;
        }
        showErrorState(state.errorMessage);
    }

    private void renderCurrentState() {
        TabState state = getCurrentState();
        binding.progressInitial.setVisibility(state.isLoading && state.items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.progressLoadMore.setVisibility(state.isLoading && !state.items.isEmpty() ? View.VISIBLE : View.GONE);
        if (!TextUtils.isEmpty(state.errorMessage) && state.items.isEmpty()) {
            showErrorState(state.errorMessage);
            return;
        }
        adapter.submitOverviewItems(new ArrayList<>(state.items));
        showContentState();
    }

    private void showContentState() {
        boolean isEmpty = adapter.getContentItemCount() == 0;
        binding.rvSharedContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(currentTab.getEmptyTitleResId());
        binding.tvEmptySubtitle.setText(currentTab.getEmptySubtitleResId());
    }

    private void showErrorState(String message) {
        binding.rvSharedContent.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(message);
        binding.progressInitial.setVisibility(View.GONE);
        binding.progressLoadMore.setVisibility(View.GONE);
    }

    @NonNull
    private TabState getCurrentState() {
        TabState state = tabStates.get(currentTab);
        if (state == null) {
            state = new TabState();
            tabStates.put(currentTab, state);
        }
        return state;
    }

    @Override
    public void onOpenItem(@NonNull DmOverviewItem item) {
        String target = item.getUrl();
        if (TextUtils.isEmpty(target)) {
            Snackbar.make(binding.getRoot(), R.string.dm_gallery_open_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (item.isLink()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
            } catch (Exception ignored) {
                Snackbar.make(binding.getRoot(), R.string.dm_gallery_open_failed, Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        if (item.isMedia() && !item.isVideo()) {
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, target);
            intent.putExtra(ImageViewerActivity.EXTRA_FILE_NAME, item.getTitle());
            startActivity(intent);
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(target), resolveMimeType(item));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
            } catch (Exception ignored) {
                Snackbar.make(binding.getRoot(), R.string.dm_gallery_open_failed, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private String resolveMimeType(DmOverviewItem item) {
        if (item.isLink()) {
            return "text/html";
        }
        if (!TextUtils.isEmpty(item.getContentType())) {
            return item.getContentType();
        }
        return "*/*";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static final class TabState {
        private final List<DmOverviewItem> items = new ArrayList<>();
        private int nextPage = 0;
        private boolean hasMore = true;
        private boolean isLoading = false;
        private boolean hasLoaded = false;
        @Nullable
        private String errorMessage;

        private void reset() {
            items.clear();
            nextPage = 0;
            hasMore = true;
            isLoading = false;
            hasLoaded = false;
            errorMessage = null;
        }
    }
}
