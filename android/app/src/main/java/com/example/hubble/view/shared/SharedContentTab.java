package com.example.hubble.view.shared;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;

public enum SharedContentTab {
    MEDIA("MEDIA", R.string.dm_gallery_tab_media,
            R.string.dm_gallery_empty_media_title),
    LINKS("LINK", R.string.dm_gallery_tab_links,
            R.string.dm_gallery_empty_links_title),
    FILES("FILE", R.string.dm_gallery_tab_files,
            R.string.dm_gallery_empty_files_title);

    @Nullable
    private final String requestType;
    private final int labelResId;
    private final int emptyTitleResId;

    SharedContentTab(@Nullable String requestType, int labelResId, int emptyTitleResId) {
        this.requestType = requestType;
        this.labelResId = labelResId;
        this.emptyTitleResId = emptyTitleResId;
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

    @NonNull
    public String getTag() {
        return name();
    }

    @NonNull
    public static SharedContentTab fromTag(@Nullable Object tag) {
        if (tag instanceof String) {
            try {
                return SharedContentTab.valueOf((String) tag);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return MEDIA;
    }
}
