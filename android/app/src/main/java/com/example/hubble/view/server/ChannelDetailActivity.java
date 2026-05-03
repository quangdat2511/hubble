package com.example.hubble.view.server;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.model.search.SearchAttachmentDto;
import com.example.hubble.data.repository.RepositoryCallback;
import com.example.hubble.data.repository.SearchRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.utils.LinkExtractionUtils;
import com.example.hubble.view.dm.DmOverviewItem;
import com.example.hubble.view.shared.SharedContentDetailsActivity;
import com.example.hubble.view.shared.SharedContentTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChannelDetailActivity extends SharedContentDetailsActivity {

    private static final String EXTRA_SERVER_ID = "extra_server_id";
    private static final String EXTRA_SERVER_NAME = "extra_server_name";
    private static final String EXTRA_SERVER_ICON_URL = "extra_server_icon_url";
    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_CHANNEL_NAME = "extra_channel_name";
    private static final String EXTRA_CHANNEL_TOPIC = "extra_channel_topic";

    private static final int LINK_MESSAGES_PAGE_SIZE = 50;

    private DmRepository dmRepository;
    private SearchRepository searchRepository;
    private ServerRepository serverRepository;
    private TokenManager tokenManager;

    private String serverId;
    private String serverName;
    private String serverIconUrl;
    private String channelId;
    private String channelName;
    private String channelTopic;

    public static Intent createIntent(Context context,
                                      String serverId,
                                      @Nullable String serverName,
                                      @Nullable String serverIconUrl,
                                      String channelId,
                                      @Nullable String channelName,
                                      @Nullable String channelTopic) {
        Intent intent = new Intent(context, ChannelDetailActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_SERVER_NAME, serverName);
        intent.putExtra(EXTRA_SERVER_ICON_URL, serverIconUrl);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_CHANNEL_TOPIC, channelTopic);
        return intent;
    }

    @Override
    protected void onScreenReady() {
        dmRepository = new DmRepository(this);
        searchRepository = new SearchRepository(this);
        serverRepository = new ServerRepository(this);
        tokenManager = new TokenManager(this);

        Intent intent = getIntent();
        serverId = intent.getStringExtra(EXTRA_SERVER_ID);
        serverName = intent.getStringExtra(EXTRA_SERVER_NAME);
        serverIconUrl = intent.getStringExtra(EXTRA_SERVER_ICON_URL);
        channelId = intent.getStringExtra(EXTRA_CHANNEL_ID);
        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME);
        channelTopic = intent.getStringExtra(EXTRA_CHANNEL_TOPIC);

        if (TextUtils.isEmpty(channelName)) {
            channelName = getString(R.string.channel_untitled);
        }

        refreshHeader();
        loadChannelDetails();
    }

    @Override
    protected void bindHeader(@NonNull SharedContentTab currentTab) {
        String cleanChannelName = sanitizeChannelName(channelName);
        String displayName = "#" + cleanChannelName;
        String subtitle = !TextUtils.isEmpty(serverName)
                ? serverName
                : getString(R.string.channel_gallery_server_fallback);
        String summary = !TextUtils.isEmpty(channelTopic)
                ? channelTopic.trim()
                : getString(R.string.channel_gallery_summary, "#" + cleanChannelName);

        binding.tvToolbarTitle.setText(displayName);
        binding.tvToolbarSubtitle.setText(getString(
                R.string.dm_gallery_toolbar_subtitle,
                getString(currentTab.getLabelResId())
        ));
        binding.tvHeaderName.setText(displayName);
        binding.tvHeaderSubtitle.setText(subtitle);
        binding.tvSharedSummary.setText(summary);

        bindChannelAvatar();
    }

    @NonNull
    @Override
    protected List<SharedContentTab> getAvailableTabs() {
        return Arrays.asList(SharedContentTab.MEDIA, SharedContentTab.LINKS, SharedContentTab.FILES);
    }

    @Override
    protected void requestTabPage(@NonNull SharedContentTab tab, int page, int size,
                                  @NonNull RepositoryCallback<LoadResult> callback) {
        if (TextUtils.isEmpty(channelId)) {
            callback.onResult(AuthResult.error(getGenericLoadErrorMessage()));
            return;
        }

        if (tab == SharedContentTab.MEDIA) {
            if (page > 0) {
                callback.onResult(AuthResult.success(new LoadResult(new ArrayList<>(), false)));
                return;
            }
            searchRepository.searchChannelMedia(channelId,
                    result -> callback.onResult(mapAttachmentResult(result, true)));
            return;
        }

        if (tab == SharedContentTab.FILES) {
            if (page > 0) {
                callback.onResult(AuthResult.success(new LoadResult(new ArrayList<>(), false)));
                return;
            }
            searchRepository.searchChannelFiles(channelId,
                    result -> callback.onResult(mapAttachmentResult(result, false)));
            return;
        }

        if (tab == SharedContentTab.LINKS) {
            dmRepository.getMessages(channelId, page, LINK_MESSAGES_PAGE_SIZE,
                    result -> callback.onResult(mapLinkResult(result)));
            return;
        }

        callback.onResult(AuthResult.error(getGenericLoadErrorMessage()));
    }

    @NonNull
    @Override
    protected String getGenericLoadErrorMessage() {
        return getString(R.string.channel_gallery_error_generic);
    }

    @Override
    protected int getEmptySubtitleResId(@NonNull SharedContentTab tab) {
        switch (tab) {
            case LINKS:
                return R.string.channel_gallery_empty_links_subtitle;
            case FILES:
                return R.string.channel_gallery_empty_files_subtitle;
            case MEDIA:
            default:
                return R.string.channel_gallery_empty_media_subtitle;
        }
    }

    @Nullable
    @Override
    protected String getAccessTokenRaw() {
        return tokenManager != null ? tokenManager.getAccessToken() : null;
    }

    @Override
    protected boolean shouldContinueLoadingAfterResult(@NonNull SharedContentTab tab,
                                                       @NonNull LoadResult loadResult,
                                                       int totalItemsAfterAppend) {
        return tab == SharedContentTab.LINKS
                && totalItemsAfterAppend == 0
                && loadResult.getItems().isEmpty()
                && loadResult.hasMore();
    }

    private void loadChannelDetails() {
        if (TextUtils.isEmpty(serverId) || TextUtils.isEmpty(channelId)) {
            return;
        }
        serverRepository.getServerChannels(serverId, result -> {
            if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
                return;
            }
            for (ChannelDto channel : result.getData()) {
                if (channel != null && channelId.equals(channel.getId())) {
                    runOnUiThread(() -> applyChannel(channel));
                    return;
                }
            }
        });
    }

    private void applyChannel(@NonNull ChannelDto channel) {
        channelName = firstNonBlank(channel.getName(), channelName, getString(R.string.channel_untitled));
        channelTopic = firstNonBlank(channel.getTopic(), channelTopic);
        refreshHeader();
    }

    @NonNull
    private AuthResult<LoadResult> mapAttachmentResult(@NonNull AuthResult<List<SearchAttachmentDto>> result,
                                                       boolean expectMedia) {
        if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
            return AuthResult.error(!TextUtils.isEmpty(result.getMessage())
                    ? result.getMessage()
                    : getGenericLoadErrorMessage());
        }

        List<DmOverviewItem> items = new ArrayList<>();
        for (SearchAttachmentDto attachment : result.getData()) {
            if (attachment == null) {
                continue;
            }
            if (!TextUtils.isEmpty(attachment.getChannelId()) && !TextUtils.equals(channelId, attachment.getChannelId())) {
                continue;
            }
            DmOverviewItem item = DmOverviewItem.fromSearchAttachment(this, attachment);
            if (expectMedia && item.isMedia()) {
                items.add(item);
            } else if (!expectMedia && !item.isMedia()) {
                items.add(item);
            }
        }
        return AuthResult.success(new LoadResult(items, false));
    }

    @NonNull
    private AuthResult<LoadResult> mapLinkResult(@NonNull AuthResult<List<MessageDto>> result) {
        if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
            return AuthResult.error(!TextUtils.isEmpty(result.getMessage())
                    ? result.getMessage()
                    : getGenericLoadErrorMessage());
        }

        List<DmOverviewItem> items = new ArrayList<>();
        List<MessageDto> messages = result.getData();
        for (MessageDto message : messages) {
            if (message == null || !TextUtils.equals(channelId, message.getChannelId())) {
                continue;
            }
            List<String> links = LinkExtractionUtils.extractLinks(message.getContent());
            String sender = firstNonBlank(message.getAuthorDisplayName(), message.getAuthorUsername());
            String snippet = firstNonBlank(message.getContent());
            String supportingText = TextUtils.isEmpty(sender) ? snippet : sender + ": " + snippet;
            for (String link : links) {
                items.add(DmOverviewItem.fromExtractedLink(
                        this,
                        message.getId(),
                        link,
                        supportingText,
                        message.getCreatedAt()
                ));
            }
        }

        return AuthResult.success(new LoadResult(items, messages.size() >= LINK_MESSAGES_PAGE_SIZE));
    }

    private void bindChannelAvatar() {
        if (!TextUtils.isEmpty(serverIconUrl)) {
            binding.ivHeaderAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.ivHeaderAvatar.setPadding(0, 0, 0, 0);
            binding.ivHeaderAvatar.setBackground(null);
            Glide.with(this)
                    .load(NetworkConfig.resolveUrl(serverIconUrl))
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .circleCrop()
                    .into(binding.ivHeaderAvatar);
            return;
        }

        binding.ivHeaderAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int iconPadding = (int) (18 * getResources().getDisplayMetrics().density);
        binding.ivHeaderAvatar.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        binding.ivHeaderAvatar.setBackgroundResource(R.drawable.bg_channel_hash_circle);
        binding.ivHeaderAvatar.setImageResource(R.drawable.ic_hashtag);
    }

    @NonNull
    private String sanitizeChannelName(@Nullable String rawName) {
        String value = rawName == null ? "" : rawName.trim();
        while (value.startsWith("#")) {
            value = value.substring(1).trim();
        }
        return value.isEmpty() ? getString(R.string.channel_untitled) : value;
    }
}
