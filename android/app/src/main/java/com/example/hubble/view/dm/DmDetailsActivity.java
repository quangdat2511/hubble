package com.example.hubble.view.dm;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.RepositoryCallback;
import com.example.hubble.view.shared.SharedContentDetailsActivity;
import com.example.hubble.view.shared.SharedContentTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DmDetailsActivity extends SharedContentDetailsActivity {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    private static final String EXTRA_USERNAME = "extra_username";
    private static final String EXTRA_AVATAR_URL = "extra_avatar_url";

    private DmRepository dmRepository;

    private String channelId;
    private String peerDisplayName;
    private String peerUsername;
    private String peerAvatarUrl;

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
    protected void onScreenReady() {
        dmRepository = new DmRepository(this);

        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        peerDisplayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        peerUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        peerAvatarUrl = getIntent().getStringExtra(EXTRA_AVATAR_URL);

        refreshHeader();
        loadPeerProfile();
    }

    @Override
    protected void bindHeader(@NonNull SharedContentTab currentTab) {
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
        if (tab.getRequestType() == null) {
            callback.onResult(AuthResult.success(new LoadResult(new ArrayList<>(), false)));
            return;
        }

        dmRepository.getSharedContent(channelId, tab.getRequestType(), page, size,
                result -> callback.onResult(mapSharedContentPageResult(result, getGenericLoadErrorMessage())));
    }

    @NonNull
    @Override
    protected String getGenericLoadErrorMessage() {
        return getString(R.string.dm_gallery_error_generic);
    }

    @Override
    protected int getEmptySubtitleResId(@NonNull SharedContentTab tab) {
        switch (tab) {
            case LINKS:
                return R.string.dm_gallery_empty_links_subtitle;
            case FILES:
                return R.string.dm_gallery_empty_files_subtitle;
            case MEDIA:
            default:
                return R.string.dm_gallery_empty_media_subtitle;
        }
    }

    @Nullable
    @Override
    protected String getAccessTokenRaw() {
        return dmRepository != null ? dmRepository.getAccessTokenRaw() : null;
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
                        peerUsername = firstNonBlank(channel.getPeerUsername(), peerUsername, peerDisplayName);
                        peerAvatarUrl = firstNonBlank(channel.getPeerAvatarUrl(), peerAvatarUrl);
                        refreshHeader();
                    });
                    return;
                }
            }
        });
    }
}
