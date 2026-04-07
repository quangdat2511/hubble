package com.example.hubble.view.dm;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.example.hubble.utils.MediaDownloadHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DmDetailsActivity extends AppCompatActivity implements DmConversationOverviewAdapter.Listener {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    private static final String EXTRA_USERNAME = "extra_username";
    private static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    private static final int SHARED_CONTENT_PAGE_SIZE = 24;
    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_IMAGE = "IMAGE";
    private static final String FILTER_VIDEO = "VIDEO";
    private static final String FILTER_DOCUMENT = "DOCUMENT";
    private static final String FILTER_AUDIO = "AUDIO";
    private static final String FILTER_ARCHIVE = "ARCHIVE";
    private static final String FILTER_OTHER = "OTHER";

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
    private boolean isUpdatingFilterChips = false;

    private final Map<Long, DownloadRequestInfo> activeDownloads = new HashMap<>();
    private final Set<String> activeItemDownloads = new HashSet<>();
    private boolean downloadReceiverRegistered = false;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                return;
            }
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            DownloadRequestInfo requestInfo = activeDownloads.remove(downloadId);
            if (requestInfo == null) {
                return;
            }
            activeItemDownloads.remove(requestInfo.itemStableId);
            adapter.setDownloadingIds(activeItemDownloads);
            handleDownloadResult(downloadId, requestInfo.fileName);
        }
    };

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
        setupFilters();
        setupRecyclerView();
        binding.btnRetry.setOnClickListener(v -> refreshContent(true));

        loadPeerProfile();
        refreshContent(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerDownloadReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterDownloadReceiver();
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
        updateFilterChips();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = DmDetailsTab.fromTag(tab.getTag());
                bindPeerHeader();
                updateFilterChips();
                renderCurrentState();
                TabState state = getCurrentState();
                if (state.hasLoaded && shouldContinueLoadingForFilter(state)) {
                    requestNextPage();
                    return;
                }
                if (!state.hasLoaded) {
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

    private void setupFilters() {
        binding.chipGroupContentFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isUpdatingFilterChips || checkedIds.isEmpty()) {
                return;
            }
            Chip selectedChip = group.findViewById(checkedIds.get(0));
            if (selectedChip == null || selectedChip.getTag() == null) {
                return;
            }
            TabState state = getCurrentState();
            String selectedFilter = selectedChip.getTag().toString();
            if (TextUtils.equals(state.activeFilter, selectedFilter)) {
                return;
            }
            state.activeFilter = selectedFilter;
            renderCurrentState();
            if (shouldContinueLoadingForFilter(state)) {
                requestNextPage();
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
                TabState state = getCurrentState();
                if (dy <= 0 || state.isLoading || !state.hasMore) {
                    return;
                }
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 6) {
                    requestNextPage();
                }
            }
        });
        adapter.setDownloadingIds(activeItemDownloads);
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
        }

        state.errorMessage = null;
        requestNextPage();
    }

    private void requestNextPage() {
        TabState state = getCurrentState();
        if (state.isLoading || !state.hasMore) {
            return;
        }
        state.isLoading = true;
        if (state.items.isEmpty()) {
            binding.progressInitial.setVisibility(View.VISIBLE);
        } else {
            binding.progressLoadMore.setVisibility(View.VISIBLE);
        }

        if (!currentTab.usesSharedContentEndpoint()) {
            state.isLoading = false;
            state.hasLoaded = true;
            renderCurrentState();
            return;
        }

        DmDetailsTab requestedTab = currentTab;
        int requestedPage = state.nextPage;
        dmRepository.getSharedContent(channelId, requestedTab.getRequestType(), requestedPage, SHARED_CONTENT_PAGE_SIZE,
                result -> runOnUiThread(() -> handleSharedContentResult(result, requestedTab, requestedPage)));
    }

    private void handleSharedContentResult(AuthResult<SharedContentPageResponse> result,
                                           @NonNull DmDetailsTab tab,
                                           int requestedPage) {
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
            state.items.addAll(mappedItems);
            state.nextPage = requestedPage + 1;
            state.hasMore = page.hasMore();
            state.hasLoaded = true;

            if (tab == currentTab && shouldContinueLoadingForFilter(state)) {
                requestNextPage();
                return;
            }

            if (tab == currentTab) {
                renderCurrentState();
            }
            return;
        }

        handleLoadFailure(tab, state, result.getMessage());
    }
    private void handleLoadFailure(@NonNull DmDetailsTab sourceTab, @NonNull TabState state, @Nullable String message) {
        state.isLoading = false;
        state.errorMessage = message != null ? message : getString(R.string.dm_gallery_error_generic);
        if (sourceTab != currentTab) {
            return;
        }
        if (!state.items.isEmpty()) {
            Snackbar.make(binding.getRoot(), state.errorMessage, Snackbar.LENGTH_LONG).show();
            renderCurrentState();
            return;
        }
        showErrorState(state.errorMessage);
    }

    private void renderCurrentState() {
        TabState state = getCurrentState();
        updateFilterChips();
        List<DmOverviewItem> filteredItems = getFilteredItems(state);

        binding.progressInitial.setVisibility(state.isLoading && filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        binding.progressLoadMore.setVisibility(state.isLoading && !filteredItems.isEmpty() ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(state.errorMessage) && filteredItems.isEmpty()) {
            showErrorState(state.errorMessage);
            return;
        }

        adapter.setDownloadingIds(activeItemDownloads);
        adapter.submitOverviewItems(filteredItems);
        showContentState(filteredItems.isEmpty(), hasActiveFilter(state));
    }

    private void showContentState(boolean isEmpty, boolean hasActiveFilter) {
        binding.rvSharedContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(hasActiveFilter
                ? R.string.dm_gallery_empty_filtered_title
                : currentTab.getEmptyTitleResId());
        binding.tvEmptySubtitle.setText(hasActiveFilter
                ? R.string.dm_gallery_empty_filtered_subtitle
                : currentTab.getEmptySubtitleResId());
    }

    private void showErrorState(String message) {
        binding.rvSharedContent.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(message);
        binding.progressInitial.setVisibility(View.GONE);
        binding.progressLoadMore.setVisibility(View.GONE);
    }

    private void updateFilterChips() {
        List<FilterOption> filterOptions = getFilterOptionsForCurrentTab();
        boolean showFilters = filterOptions.size() > 1;
        binding.contentFiltersContainer.setVisibility(showFilters ? View.VISIBLE : View.GONE);
        binding.chipGroupContentFilters.removeAllViews();

        if (!showFilters) {
            getCurrentState().activeFilter = FILTER_ALL;
            return;
        }

        isUpdatingFilterChips = true;
        for (FilterOption option : filterOptions) {
            Chip chip = new Chip(this);
            chip.setText(option.labelResId);
            chip.setTag(option.key);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(TextUtils.equals(getCurrentState().activeFilter, option.key));
            binding.chipGroupContentFilters.addView(chip);
        }
        isUpdatingFilterChips = false;
    }

    @NonNull
    private List<FilterOption> getFilterOptionsForCurrentTab() {
        List<FilterOption> filterOptions = new ArrayList<>();
        filterOptions.add(new FilterOption(FILTER_ALL, R.string.dm_gallery_filter_all));

        if (currentTab == DmDetailsTab.MEDIA) {
            filterOptions.add(new FilterOption(FILTER_IMAGE, R.string.dm_gallery_filter_images));
            filterOptions.add(new FilterOption(FILTER_VIDEO, R.string.dm_gallery_filter_videos));
            return filterOptions;
        }

        if (currentTab == DmDetailsTab.FILES) {
            filterOptions.add(new FilterOption(FILTER_DOCUMENT, R.string.dm_gallery_filter_documents));
            filterOptions.add(new FilterOption(FILTER_AUDIO, R.string.dm_gallery_filter_audio));
            filterOptions.add(new FilterOption(FILTER_ARCHIVE, R.string.dm_gallery_filter_archives));
            filterOptions.add(new FilterOption(FILTER_OTHER, R.string.dm_gallery_filter_other));
        }
        return filterOptions;
    }

    @NonNull
    private List<DmOverviewItem> getFilteredItems(@NonNull TabState state) {
        if (!hasActiveFilter(state)) {
            return new ArrayList<>(state.items);
        }
        List<DmOverviewItem> filteredItems = new ArrayList<>();
        for (DmOverviewItem item : state.items) {
            if (matchesFilter(item, state.activeFilter)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    private boolean shouldContinueLoadingForFilter(@NonNull TabState state) {
        return currentTab == getCurrentTabFromState(state)
                && hasActiveFilter(state)
                && state.hasMore
                && getFilteredItems(state).isEmpty();
    }

    @NonNull
    private DmDetailsTab getCurrentTabFromState(@NonNull TabState state) {
        for (Map.Entry<DmDetailsTab, TabState> entry : tabStates.entrySet()) {
            if (entry.getValue() == state) {
                return entry.getKey();
            }
        }
        return currentTab;
    }

    private boolean hasActiveFilter(@NonNull TabState state) {
        return !TextUtils.isEmpty(state.activeFilter) && !FILTER_ALL.equalsIgnoreCase(state.activeFilter);
    }

    private boolean matchesFilter(@NonNull DmOverviewItem item, @NonNull String filterKey) {
        switch (filterKey) {
            case FILTER_IMAGE:
                return item.getKind() == DmOverviewItem.Kind.IMAGE;
            case FILTER_VIDEO:
                return item.getKind() == DmOverviewItem.Kind.VIDEO;
            case FILTER_DOCUMENT:
            case FILTER_AUDIO:
            case FILTER_ARCHIVE:
            case FILTER_OTHER:
                return item.isFile() && TextUtils.equals(resolveFileSubtype(item), filterKey);
            default:
                return true;
        }
    }

    @NonNull
    private String resolveFileSubtype(@NonNull DmOverviewItem item) {
        String contentType = item.getContentType() == null ? "" : item.getContentType().toLowerCase(Locale.US);
        String fileName = item.getTitle().toLowerCase(Locale.US);

        if (contentType.startsWith("audio/")
                || fileName.endsWith(".mp3")
                || fileName.endsWith(".wav")
                || fileName.endsWith(".m4a")
                || fileName.endsWith(".aac")
                || fileName.endsWith(".ogg")) {
            return FILTER_AUDIO;
        }

        if (contentType.contains("zip")
                || contentType.contains("rar")
                || contentType.contains("7z")
                || fileName.endsWith(".zip")
                || fileName.endsWith(".rar")
                || fileName.endsWith(".7z")
                || fileName.endsWith(".tar")
                || fileName.endsWith(".gz")) {
            return FILTER_ARCHIVE;
        }

        if (contentType.startsWith("text/")
                || contentType.contains("pdf")
                || contentType.contains("word")
                || contentType.contains("document")
                || contentType.contains("excel")
                || contentType.contains("spreadsheet")
                || contentType.contains("powerpoint")
                || contentType.contains("presentation")
                || fileName.endsWith(".pdf")
                || fileName.endsWith(".doc")
                || fileName.endsWith(".docx")
                || fileName.endsWith(".xls")
                || fileName.endsWith(".xlsx")
                || fileName.endsWith(".ppt")
                || fileName.endsWith(".pptx")
                || fileName.endsWith(".txt")
                || fileName.endsWith(".csv")) {
            return FILTER_DOCUMENT;
        }

        return FILTER_OTHER;
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

    @Override
    public void onDownloadItem(@NonNull DmOverviewItem item) {
        if (!item.isDownloadable()) {
            Snackbar.make(binding.getRoot(), R.string.dm_gallery_download_unsupported, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (activeItemDownloads.contains(item.getStableId())) {
            Snackbar.make(binding.getRoot(), R.string.dm_gallery_download_duplicate, Snackbar.LENGTH_SHORT).show();
            return;
        }

        try {
            String token = dmRepository.getAccessTokenRaw();
            MediaDownloadHelper.EnqueueResult enqueueResult = MediaDownloadHelper.enqueueDownload(
                    this,
                    item.getUrl(),
                    item.getTitle(),
                    resolveMimeType(item),
                    token,
                    getString(R.string.dm_gallery_download_in_progress)
            );
            activeDownloads.put(enqueueResult.getDownloadId(),
                    new DownloadRequestInfo(item.getStableId(), enqueueResult.getFileName()));
            activeItemDownloads.add(item.getStableId());
            adapter.setDownloadingIds(activeItemDownloads);
            Snackbar.make(binding.getRoot(),
                    getString(R.string.dm_gallery_download_started, enqueueResult.getFileName()),
                    Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : getString(R.string.dm_gallery_download_failed);
            Snackbar.make(binding.getRoot(),
                    getString(R.string.dm_gallery_download_failed_with_reason, message),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void handleDownloadResult(long downloadId, String fileName) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Snackbar.make(binding.getRoot(),
                        getString(R.string.dm_gallery_download_success, fileName),
                        Snackbar.LENGTH_LONG).show();
            } else {
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                Snackbar.make(binding.getRoot(),
                        getString(R.string.dm_gallery_download_failed_with_reason, String.valueOf(reason)),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void registerDownloadReceiver() {
        if (downloadReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(
                this,
                downloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        downloadReceiverRegistered = true;
    }

    private void unregisterDownloadReceiver() {
        if (!downloadReceiverRegistered) {
            return;
        }
        unregisterReceiver(downloadReceiver);
        downloadReceiverRegistered = false;
    }

    private String resolveMimeType(@NonNull DmOverviewItem item) {
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
        private String activeFilter = FILTER_ALL;
        @Nullable
        private String errorMessage;

        private void reset() {
            items.clear();
            nextPage = 0;
            hasMore = true;
            isLoading = false;
            hasLoaded = false;
            activeFilter = FILTER_ALL;
            errorMessage = null;
        }
    }

    private static final class FilterOption {
        private final String key;
        private final int labelResId;

        private FilterOption(String key, int labelResId) {
            this.key = key;
            this.labelResId = labelResId;
        }
    }

    private static final class DownloadRequestInfo {
        private final String itemStableId;
        private final String fileName;

        private DownloadRequestInfo(String itemStableId, String fileName) {
            this.itemStableId = itemStableId;
            this.fileName = fileName;
        }
    }
}
