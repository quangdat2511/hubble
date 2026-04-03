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

    public static final int VIEW_TYPE_MEDIA = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_FILE = 3;

    private final List<SharedContentItemResponse> items = new ArrayList<>();
    private final Listener listener;
    private String activeType = "MEDIA";

    public DmSharedContentAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setActiveType(@NonNull String activeType) {
        this.activeType = activeType;
        notifyDataSetChanged();
    }

    public void replaceItems(@NonNull List<SharedContentItemResponse> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendItems(@NonNull List<SharedContentItemResponse> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public int getDataItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        SharedContentItemResponse item = items.get(position);
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
        SharedContentItemResponse item = items.get(position);
        if (holder instanceof MediaViewHolder) {
            ((MediaViewHolder) holder).bind(item);
        } else if (holder instanceof LinkViewHolder) {
            ((LinkViewHolder) holder).bind(item);
        } else if (holder instanceof FileViewHolder) {
            ((FileViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
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
        if (TextUtils.isEmpty(rawDate)) {
            return "";
        }
        try {
            return OffsetDateTime.parse(rawDate).format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(rawDate).format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()));
            } catch (Exception ignoredAgain) {
                return rawDate;
            }
        }
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
}
