package com.example.hubble.adapter.dm;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.databinding.ItemDmSharedFileBinding;
import com.example.hubble.databinding.ItemDmSharedLinkBinding;
import com.example.hubble.databinding.ItemDmSharedMediaBinding;
import com.example.hubble.databinding.ItemDmSharedSectionHeaderBinding;
import com.example.hubble.databinding.ItemDmSharedTextBinding;
import com.example.hubble.view.dm.DmOverviewItem;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DmConversationOverviewAdapter extends ListAdapter<DmConversationOverviewAdapter.RowItem, RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpenItem(@NonNull DmOverviewItem item);

        void onDownloadItem(@NonNull DmOverviewItem item);
    }

    public static final int VIEW_TYPE_SECTION_HEADER = 0;
    public static final int VIEW_TYPE_MEDIA = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_FILE = 3;
    public static final int VIEW_TYPE_TEXT = 4;

    private static final DiffUtil.ItemCallback<RowItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<RowItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull RowItem oldItem, @NonNull RowItem newItem) {
            return TextUtils.equals(oldItem.stableId, newItem.stableId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull RowItem oldItem, @NonNull RowItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final Listener listener;
    private int contentItemCount = 0;
    private final Set<String> downloadingIds = new HashSet<>();

    public DmConversationOverviewAdapter(@NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submitOverviewItems(@NonNull List<DmOverviewItem> items) {
        contentItemCount = items.size();
        submitList(buildRows(items));
    }

    public int getContentItemCount() {
        return contentItemCount;
    }

    public void setDownloadingIds(@NonNull Set<String> itemIds) {
        downloadingIds.clear();
        downloadingIds.addAll(itemIds);
        if (!getCurrentList().isEmpty()) {
            notifyItemRangeChanged(0, getCurrentList().size());
        }
    }

    public GridLayoutManager.SpanSizeLookup createSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = getItemViewType(position);
                return viewType == VIEW_TYPE_MEDIA ? 1 : 3;
            }
        };
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).stableId.hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SECTION_HEADER) {
            return new SectionHeaderViewHolder(ItemDmSharedSectionHeaderBinding.inflate(inflater, parent, false));
        }
        if (viewType == VIEW_TYPE_LINK) {
            return new LinkViewHolder(ItemDmSharedLinkBinding.inflate(inflater, parent, false));
        }
        if (viewType == VIEW_TYPE_FILE) {
            return new FileViewHolder(ItemDmSharedFileBinding.inflate(inflater, parent, false));
        }
        if (viewType == VIEW_TYPE_TEXT) {
            return new TextViewHolder(ItemDmSharedTextBinding.inflate(inflater, parent, false));
        }
        return new MediaViewHolder(ItemDmSharedMediaBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowItem rowItem = getItem(position);
        if (holder instanceof SectionHeaderViewHolder) {
            ((SectionHeaderViewHolder) holder).bind(rowItem.headerLabel);
            return;
        }
        if (rowItem.item == null) {
            return;
        }
        if (holder instanceof MediaViewHolder) {
            ((MediaViewHolder) holder).bind(rowItem.item);
        } else if (holder instanceof LinkViewHolder) {
            ((LinkViewHolder) holder).bind(rowItem.item);
        } else if (holder instanceof FileViewHolder) {
            ((FileViewHolder) holder).bind(rowItem.item);
        } else if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(rowItem.item);
        }
    }

    private List<RowItem> buildRows(@NonNull List<DmOverviewItem> items) {
        List<RowItem> rows = new ArrayList<>();
        String lastHeader = null;
        for (DmOverviewItem item : items) {
            String headerLabel = buildSectionHeader(item.getCreatedAt());
            if (!TextUtils.equals(lastHeader, headerLabel)) {
                rows.add(RowItem.header(headerLabel));
                lastHeader = headerLabel;
            }
            rows.add(RowItem.item(item));
        }
        return rows;
    }

    @NonNull
    private String buildSectionHeader(String rawDate) {
        OffsetDateTime offsetDateTime = parseOffsetDateTime(rawDate);
        if (offsetDateTime != null) {
            return offsetDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        }
        LocalDateTime localDateTime = parseLocalDateTime(rawDate);
        if (localDateTime != null) {
            return localDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        }
        return "Recent";
    }

    private OffsetDateTime parseOffsetDateTime(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawDate);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return null;
        }
        try {
            return LocalDateTime.parse(rawDate);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatDate(String rawDate) {
        OffsetDateTime offsetDateTime = parseOffsetDateTime(rawDate);
        if (offsetDateTime != null) {
            return offsetDateTime.format(DateTimeFormatter.ofPattern("dd MMM • HH:mm", Locale.getDefault()));
        }
        LocalDateTime localDateTime = parseLocalDateTime(rawDate);
        if (localDateTime != null) {
            return localDateTime.format(DateTimeFormatter.ofPattern("dd MMM • HH:mm", Locale.getDefault()));
        }
        return "";
    }

    private int resolveFileIcon(@NonNull DmOverviewItem item) {
        String filename = item.getTitle().toLowerCase(Locale.US);
        String contentType = item.getContentType() == null ? "" : item.getContentType().toLowerCase(Locale.US);

        if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
            return R.drawable.ic_file_pdf;
        }
        if (contentType.contains("word") || contentType.contains("document")
                || filename.endsWith(".doc") || filename.endsWith(".docx")) {
            return R.drawable.ic_file_docx;
        }
        if (contentType.contains("excel") || contentType.contains("spreadsheet")
                || filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            return R.drawable.ic_file_excel;
        }
        if (contentType.contains("powerpoint") || contentType.contains("presentation")
                || filename.endsWith(".ppt") || filename.endsWith(".pptx")) {
            return R.drawable.ic_file_powerpoint;
        }
        if (contentType.contains("zip") || filename.endsWith(".zip") || filename.endsWith(".rar")) {
            return R.drawable.ic_file_zip;
        }
        if (contentType.startsWith("text/") || filename.endsWith(".txt")) {
            return R.drawable.ic_file_text;
        }
        return R.drawable.ic_file_generic;
    }

    private final class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmSharedSectionHeaderBinding binding;

        private SectionHeaderViewHolder(ItemDmSharedSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(String title) {
            binding.tvSectionHeader.setText(title);
        }
    }

    private final class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmSharedMediaBinding binding;

        private MediaViewHolder(ItemDmSharedMediaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DmOverviewItem item) {
            binding.tvMediaLabel.setText(item.getTitle());
            binding.tvMediaDate.setText(formatDate(item.getCreatedAt()));
            binding.ivVideoBadge.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
            boolean downloading = downloadingIds.contains(item.getStableId());
            binding.btnDownloadMedia.setVisibility(item.isDownloadable() ? View.VISIBLE : View.GONE);
            binding.btnDownloadMedia.setEnabled(!downloading);
            binding.btnDownloadMedia.setAlpha(downloading ? 0.45f : 1f);

            Glide.with(binding.ivMediaThumb.getContext())
                    .load(item.getPreviewUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.ivMediaThumb);

            binding.getRoot().setOnClickListener(v -> listener.onOpenItem(item));
            binding.btnDownloadMedia.setOnClickListener(v -> listener.onDownloadItem(item));
        }
    }

    private final class LinkViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmSharedLinkBinding binding;

        private LinkViewHolder(ItemDmSharedLinkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DmOverviewItem item) {
            binding.tvLinkTitle.setText(item.getTitle());
            binding.tvLinkUrl.setText(item.getUrl());
            binding.tvLinkSnippet.setText(TextUtils.isEmpty(item.getSupportingText())
                    ? binding.getRoot().getContext().getString(R.string.dm_gallery_link_item_hint)
                    : item.getSupportingText());
            binding.tvLinkDate.setText(formatDate(item.getCreatedAt()));
            binding.getRoot().setOnClickListener(v -> listener.onOpenItem(item));
        }
    }

    private final class FileViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmSharedFileBinding binding;

        private FileViewHolder(ItemDmSharedFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DmOverviewItem item) {
            binding.tvFileName.setText(item.getTitle());
            binding.tvFileMeta.setText(item.getSupportingText());
            binding.tvFileDate.setText(formatDate(item.getCreatedAt()));
            binding.ivFileIcon.setImageResource(resolveFileIcon(item));
            boolean downloading = downloadingIds.contains(item.getStableId());
            binding.btnDownloadFile.setVisibility(item.isDownloadable() ? View.VISIBLE : View.GONE);
            binding.btnDownloadFile.setEnabled(!downloading);
            binding.btnDownloadFile.setAlpha(downloading ? 0.45f : 1f);
            binding.getRoot().setOnClickListener(v -> listener.onOpenItem(item));
            binding.btnDownloadFile.setOnClickListener(v -> listener.onDownloadItem(item));
        }
    }

    private final class TextViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmSharedTextBinding binding;

        private TextViewHolder(ItemDmSharedTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DmOverviewItem item) {
            binding.tvPinnedTitle.setText(item.isPinned()
                    ? binding.getRoot().getContext().getString(R.string.dm_gallery_pinned_badge)
                    : item.getTitle());
            binding.tvPinnedBody.setText(item.getSupportingText());
            binding.tvPinnedDate.setText(formatDate(item.getCreatedAt()));
            binding.getRoot().setOnClickListener(null);
        }
    }

    static final class RowItem {
        private final String stableId;
        private final int viewType;
        private final String headerLabel;
        private final DmOverviewItem item;

        private RowItem(String stableId, int viewType, String headerLabel, DmOverviewItem item) {
            this.stableId = stableId;
            this.viewType = viewType;
            this.headerLabel = headerLabel;
            this.item = item;
        }

        private static RowItem header(String label) {
            return new RowItem("header:" + label, VIEW_TYPE_SECTION_HEADER, label, null);
        }

        private static RowItem item(DmOverviewItem item) {
            int viewType = VIEW_TYPE_TEXT;
            if (item.isMedia()) {
                viewType = VIEW_TYPE_MEDIA;
            } else if (item.isLink()) {
                viewType = VIEW_TYPE_LINK;
            } else if (item.isFile()) {
                viewType = VIEW_TYPE_FILE;
            }
            return new RowItem(item.getStableId(), viewType, null, item);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RowItem)) {
                return false;
            }
            RowItem item = (RowItem) other;
            return viewType == item.viewType
                    && TextUtils.equals(stableId, item.stableId)
                    && TextUtils.equals(headerLabel, item.headerLabel)
                    && compareOverviewItems(this.item, item.item);
        }

        @Override
        public int hashCode() {
            int result = stableId.hashCode();
            result = 31 * result + viewType;
            result = 31 * result + (headerLabel != null ? headerLabel.hashCode() : 0);
            result = 31 * result + (item != null ? item.getStableId().hashCode() : 0);
            return result;
        }

        private static boolean compareOverviewItems(DmOverviewItem left, DmOverviewItem right) {
            if (left == right) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            return left.getKind() == right.getKind()
                    && TextUtils.equals(left.getStableId(), right.getStableId())
                    && TextUtils.equals(left.getTitle(), right.getTitle())
                    && TextUtils.equals(left.getSupportingText(), right.getSupportingText())
                    && TextUtils.equals(left.getUrl(), right.getUrl())
                    && TextUtils.equals(left.getPreviewUrl(), right.getPreviewUrl())
                    && TextUtils.equals(left.getContentType(), right.getContentType())
                    && TextUtils.equals(left.getCreatedAt(), right.getCreatedAt())
                    && left.getSizeBytes() == right.getSizeBytes()
                    && left.isPinned() == right.isPinned();
        }
    }
}
