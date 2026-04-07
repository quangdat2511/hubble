package com.example.hubble.view.dm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;

public enum DmDetailsTab {
    MEDIA("MEDIA", R.string.dm_gallery_tab_media,
            R.string.dm_gallery_empty_media_title,
            R.string.dm_gallery_empty_media_subtitle),
    LINKS("LINK", R.string.dm_gallery_tab_links,
            R.string.dm_gallery_empty_links_title,
            R.string.dm_gallery_empty_links_subtitle),
    FILES("FILE", R.string.dm_gallery_tab_files,
            R.string.dm_gallery_empty_files_title,
            R.string.dm_gallery_empty_files_subtitle);

    @Nullable
    private final String requestType;
    private final int labelResId;
    private final int emptyTitleResId;
    private final int emptySubtitleResId;

    DmDetailsTab(@Nullable String requestType, int labelResId, int emptyTitleResId, int emptySubtitleResId) {
        this.requestType = requestType;
        this.labelResId = labelResId;
        this.emptyTitleResId = emptyTitleResId;
        this.emptySubtitleResId = emptySubtitleResId;
    }

    @Nullable
    public String getRequestType() {
        return requestType;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public int getEmptyTitleResId() {
        return emptyTitleResId;
    }

    public int getEmptySubtitleResId() {
        return emptySubtitleResId;
    }

    public boolean usesSharedContentEndpoint() {
        return requestType != null;
    }

    @NonNull
    public String getTag() {
        return name();
    }

    @NonNull
    public static DmDetailsTab fromTag(@Nullable Object tag) {
        if (tag instanceof String) {
            try {
                return DmDetailsTab.valueOf((String) tag);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return MEDIA;
    }
}
