package com.example.hubble.view.dm;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmSharedContentAdapter;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.SharedContentItemResponse;
import com.example.hubble.data.model.dm.SharedContentPageResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmDetailsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DmDetailsActivity extends AppCompatActivity implements DmSharedContentAdapter.Listener {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    private static final String EXTRA_USERNAME = "extra_username";
    private static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    private static final int PAGE_SIZE = 24;

    private ActivityDmDetailsBinding binding;
    private DmRepository dmRepository;
    private DmSharedContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private String channelId;
    private String peerDisplayName;
    private String peerUsername;
    private String peerAvatarUrl;
    private String currentType = "MEDIA";
    private int currentPage = 0;
    private boolean hasMore = true;
    private boolean isLoading = false;

    private final Map<Long, String> activeDownloads = new HashMap<>();
    private boolean downloadReceiverRegistered = false;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                return;
            }
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (downloadId < 0 || !activeDownloads.containsKey(downloadId)) {
                return;
            }
            String fileName = activeDownloads.remove(downloadId);
            handleDownloadResult(downloadId, fileName);
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
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.dm_gallery_tab_media).setTag("MEDIA"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.dm_gallery_tab_links).setTag("LINK"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.dm_gallery_tab_files).setTag("FILE"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                currentType = tag == null ? "MEDIA" : tag.toString();
                refreshContent(true);
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
        adapter = new DmSharedContentAdapter(this);
        layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItemViewType(position) == DmSharedContentAdapter.VIEW_TYPE_MEDIA ? 1 : 3;
            }
        });

        binding.rvSharedContent.setLayoutManager(layoutManager);
        binding.rvSharedContent.setAdapter(adapter);
        binding.rvSharedContent.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoading || !hasMore) {
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
        binding.tvToolbarSubtitle.setText(getString(R.string.dm_gallery_toolbar_subtitle, getTabLabel(currentType)));

        Glide.with(this)
                .load(peerAvatarUrl)
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

        if (reset) {
            currentPage = 0;
            hasMore = true;
            adapter.replaceItems(new ArrayList<>());
            binding.progressInitial.setVisibility(View.VISIBLE);
            binding.rvSharedContent.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.layoutErrorState.setVisibility(View.GONE);
        } else {
            binding.progressLoadMore.setVisibility(View.VISIBLE);
        }

        isLoading = true;
        binding.tvToolbarSubtitle.setText(getString(R.string.dm_gallery_toolbar_subtitle, getTabLabel(currentType)));

        dmRepository.getSharedContent(channelId, currentType, currentPage, PAGE_SIZE,
                result -> runOnUiThread(() -> handleSharedContentResult(result, reset)));
    }

    private void loadNextPage() {
        if (isLoading || !hasMore) {
            return;
        }
        currentPage += 1;
        refreshContent(false);
    }

    private void handleSharedContentResult(AuthResult<SharedContentPageResponse> result, boolean reset) {
        isLoading = false;
        binding.progressInitial.setVisibility(View.GONE);
        binding.progressLoadMore.setVisibility(View.GONE);

        if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
            SharedContentPageResponse page = result.getData();
            List<SharedContentItemResponse> items = page.getItems();
            hasMore = page.hasMore();

            if (reset) {
                adapter.replaceItems(items);
            } else {
                adapter.appendItems(items);
            }
            showContentState();
            return;
        }

        if (!reset && currentPage > 0) {
            currentPage -= 1;
            Snackbar.make(binding.getRoot(),
                    result.getMessage() != null ? result.getMessage() : getString(R.string.dm_gallery_error_generic),
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        showErrorState(result.getMessage() != null ? result.getMessage() : getString(R.string.dm_gallery_error_generic));
    }

    private void showContentState() {
        boolean isEmpty = adapter.getDataItemCount() == 0;
        binding.rvSharedContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(getEmptyTitle());
        binding.tvEmptySubtitle.setText(getEmptySubtitle());
    }

    private void showErrorState(String message) {
        binding.rvSharedContent.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(message);
    }

    private String getEmptyTitle() {
        if ("LINK".equalsIgnoreCase(currentType)) {
            return getString(R.string.dm_gallery_empty_links_title);
        }
        if ("FILE".equalsIgnoreCase(currentType)) {
            return getString(R.string.dm_gallery_empty_files_title);
        }
        return getString(R.string.dm_gallery_empty_media_title);
    }

    private String getEmptySubtitle() {
        if ("LINK".equalsIgnoreCase(currentType)) {
            return getString(R.string.dm_gallery_empty_links_subtitle);
        }
        if ("FILE".equalsIgnoreCase(currentType)) {
            return getString(R.string.dm_gallery_empty_files_subtitle);
        }
        return getString(R.string.dm_gallery_empty_media_subtitle);
    }

    private String getTabLabel(String type) {
        if ("LINK".equalsIgnoreCase(type)) {
            return getString(R.string.dm_gallery_tab_links);
        }
        if ("FILE".equalsIgnoreCase(type)) {
            return getString(R.string.dm_gallery_tab_files);
        }
        return getString(R.string.dm_gallery_tab_media);
    }

    @Override
    public void onOpenItem(@NonNull SharedContentItemResponse item) {
        String target = normalizeUrl(item.getUrl());
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
            intent.putExtra(ImageViewerActivity.EXTRA_FILE_NAME, item.getResolvedFileName());
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
    public void onDownloadItem(@NonNull SharedContentItemResponse item) {
        String target = normalizeUrl(item.getUrl());
        if (TextUtils.isEmpty(target)) {
            Snackbar.make(binding.getRoot(), R.string.dm_gallery_download_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }

        String fileName = sanitizeFileName(item.getResolvedFileName());

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(target));
            request.setTitle(fileName);
            request.setDescription(getString(R.string.dm_gallery_download_in_progress));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            String token = dmRepository.getAccessTokenRaw();
            if (!TextUtils.isEmpty(token)) {
                request.addRequestHeader("Authorization", "Bearer " + token);
            }

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Snackbar.make(binding.getRoot(), R.string.dm_gallery_download_failed, Snackbar.LENGTH_SHORT).show();
                return;
            }

            long downloadId = downloadManager.enqueue(request);
            activeDownloads.put(downloadId, fileName);
            Snackbar.make(binding.getRoot(),
                    getString(R.string.dm_gallery_download_started, fileName),
                    Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.dm_gallery_download_failed_with_reason, e.getMessage()),
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
        downloadReceiverRegistered = true;
    }

    private void unregisterDownloadReceiver() {
        if (!downloadReceiverRegistered) {
            return;
        }
        unregisterReceiver(downloadReceiver);
        downloadReceiverRegistered = false;
    }

    private String resolveMimeType(SharedContentItemResponse item) {
        if (item.isLink()) {
            return "text/html";
        }
        if (!TextUtils.isEmpty(item.getContentType())) {
            return item.getContentType();
        }
        return "*/*";
    }

    private String normalizeUrl(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return null;
        }
        String normalized = rawUrl.replace("localhost", "10.0.2.2");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return URLUtil.guessUrl(normalized);
    }

    private String sanitizeFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "download";
        }
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "download" : sanitized;
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
}
