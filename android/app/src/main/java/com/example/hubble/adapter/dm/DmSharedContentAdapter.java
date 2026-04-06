package com.example.hubble.adapter.dm;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.model.dm.SharedContentItemResponse;
import com.example.hubble.databinding.ItemDmSharedFileBinding;
import com.example.hubble.databinding.ItemDmSharedLinkBinding;
import com.example.hubble.databinding.ItemDmSharedMediaBinding;
import com.example.hubble.databinding.ItemDmSharedSectionHeaderBinding;

import java.net.URI;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DmSharedContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpenItem(@NonNull SharedContentItemResponse item);

        void onDownloadItem(@NonNull SharedContentItemResponse item);
    }

    public static final int VIEW_TYPE_SECTION_HEADER = 0;
    public static final int VIEW_TYPE_MEDIA = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_FILE = 3;

    private final List<SharedContentItemResponse> dataItems = new ArrayList<>();
    private final List<DisplayItem> displayItems = new ArrayList<>();
    private final Listener listener;

    public DmSharedContentAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void replaceItems(@NonNull List<SharedContentItemResponse> newItems) {
        dataItems.clear();
        dataItems.addAll(newItems);
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public void appendItems(@NonNull List<SharedContentItemResponse> newItems) {
        dataItems.addAll(newItems);
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public int getDataItemCount() {
        return dataItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        DisplayItem displayItem = displayItems.get(position);
        if (displayItem.isHeader()) {
            return VIEW_TYPE_SECTION_HEADER;
        }
        SharedContentItemResponse item = displayItem.item;
        if (item.isLink()) {
            return VIEW_TYPE_LINK;
        }
        if (item.isFile()) {
            return VIEW_TYPE_FILE;
        }
        return VIEW_TYPE_MEDIA;
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
        return new MediaViewHolder(ItemDmSharedMediaBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem displayItem = displayItems.get(position);
        if (holder instanceof SectionHeaderViewHolder) {
            ((SectionHeaderViewHolder) holder).bind(displayItem.headerLabel);
            return;
        }
        SharedContentItemResponse item = displayItem.item;
        if (holder instanceof MediaViewHolder && item != null) {
            ((MediaViewHolder) holder).bind(item);
        } else if (holder instanceof LinkViewHolder && item != null) {
            ((LinkViewHolder) holder).bind(item);
        } else if (holder instanceof FileViewHolder && item != null) {
            ((FileViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        String lastHeader = null;
        for (SharedContentItemResponse item : dataItems) {
            String headerLabel = buildSectionHeader(item.getCreatedAt());
            if (!TextUtils.equals(lastHeader, headerLabel)) {
                displayItems.add(DisplayItem.header(headerLabel));
                lastHeader = headerLabel;
            }
            displayItems.add(DisplayItem.item(item));
        }
    }

    private String buildSectionHeader(String rawDate) {
        OffsetDateTime offsetDateTime = parseOffsetDateTime(rawDate);
        if (offsetDateTime != null) {
            return offsetDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        }

        LocalDateTime localDateTime = parseLocalDateTime(rawDate);
        if (localDateTime != null) {
            return localDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        }
        return rawDate == null ? "" : rawDate;
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

        private void bind(SharedContentItemResponse item) {
            binding.tvMediaLabel.setText(item.getResolvedFileName());
            binding.tvMediaDate.setText(formatDate(item.getCreatedAt()));
            binding.ivVideoBadge.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

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

        private void bind(SharedContentItemResponse item) {
            binding.tvLinkTitle.setText(extractHost(item.getUrl()));
            binding.tvLinkUrl.setText(item.getUrl());
            String snippet = TextUtils.isEmpty(item.getMessageContent())
                    ? binding.getRoot().getContext().getString(R.string.dm_gallery_link_item_hint)
                    : item.getMessageContent();
            binding.tvLinkSnippet.setText(snippet);
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

        private void bind(SharedContentItemResponse item) {
            binding.tvFileName.setText(item.getResolvedFileName());
            binding.tvFileMeta.setText(buildFileMeta(item));
            binding.tvFileDate.setText(formatDate(item.getCreatedAt()));
            binding.ivFileIcon.setImageResource(resolveFileIcon(item));
            binding.getRoot().setOnClickListener(v -> listener.onOpenItem(item));
            binding.btnDownloadFile.setOnClickListener(v -> listener.onDownloadItem(item));
        }
    }

    private int resolveFileIcon(SharedContentItemResponse item) {
        String filename = item.getResolvedFileName().toLowerCase(Locale.US);
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

    private String buildFileMeta(SharedContentItemResponse item) {
        String contentType = item.getContentType();
        String sizeText = formatFileSize(item.getSizeBytes());
        if (!TextUtils.isEmpty(contentType)) {
            return contentType + " • " + sizeText;
        }
        return sizeText;
    }

    private String formatFileSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            return "Unknown size";
        }
        double size = sizeBytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024d;
            unitIndex++;
        }
        return new DecimalFormat(size >= 10 ? "0" : "0.0").format(size) + " " + units[unitIndex];
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

        return rawDate == null ? "" : rawDate;
    }

    private String extractHost(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return "";
        }
        try {
            URI uri = URI.create(URLUtil.guessUrl(rawUrl));
            if (!TextUtils.isEmpty(uri.getHost())) {
                return uri.getHost();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return rawUrl;
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

    private static final class DisplayItem {
        private final String headerLabel;
        private final SharedContentItemResponse item;

        private DisplayItem(String headerLabel, SharedContentItemResponse item) {
            this.headerLabel = headerLabel;
            this.item = item;
        }

        private static DisplayItem header(String title) {
            return new DisplayItem(title, null);
        }

        private static DisplayItem item(SharedContentItemResponse item) {
            return new DisplayItem(null, item);
        }

        private boolean isHeader() {
            return item == null;
        }
    }
}
