package com.example.hubble.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.model.AttachmentResponse;
import com.example.hubble.data.model.DmMessageItem;
import com.example.hubble.databinding.ItemDmMessageMeBinding;
import com.example.hubble.databinding.ItemDmMessageOtherBinding;

import java.util.ArrayList;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<DmMessageItem> items = new ArrayList<>();

    public void setItems(List<DmMessageItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isMine() ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            return new MeHolder(ItemDmMessageMeBinding.inflate(inflater, parent, false));
        }
        return new OtherHolder(ItemDmMessageOtherBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DmMessageItem item = items.get(position);
        if (holder instanceof MeHolder) ((MeHolder) holder).bind(item);
        else ((OtherHolder) holder).bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── Shared utility ────────────────────────────────────────────────────────

    private static void loadAttachments(LinearLayout container, List<AttachmentResponse> attachments) {
        // Always clear recycled views first
        container.removeAllViews();

        if (attachments == null || attachments.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        boolean hasAny = false;
        for (AttachmentResponse att : attachments) {
            if (att.getContentType() == null || !att.getContentType().startsWith("image/")) continue;

            hasAny = true;
            ImageView iv = new ImageView(container.getContext());

            float density = container.getContext().getResources().getDisplayMetrics().density;
            int w = Math.round(200 * density);
            int h = Math.round(150 * density);
            int gap = Math.round(4 * density);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, h);
            params.setMargins(0, 0, 0, gap);
            iv.setLayoutParams(params);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String url = att.getUrl() == null ? ""
                    : att.getUrl().replace("localhost", "10.0.2.2");

            Glide.with(container.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(iv);

            container.addView(iv);
        }

        container.setVisibility(hasAny ? View.VISIBLE : View.GONE);
    }

    // ── MeHolder ──────────────────────────────────────────────────────────────

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding binding;

        MeHolder(ItemDmMessageMeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvTime.setText(item.getTimestamp());

            if (item.getContent() != null && !item.getContent().isEmpty()) {
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(item.getContent());
            } else {
                binding.tvMessage.setVisibility(View.GONE);
            }

            loadAttachments(binding.llAttachments, item.getAttachments());
        }
    }

    // ── OtherHolder ───────────────────────────────────────────────────────────

    static class OtherHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageOtherBinding binding;

        OtherHolder(ItemDmMessageOtherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvName.setText(item.getSenderName());
            binding.tvTime.setText(item.getTimestamp());

            if (item.getContent() != null && !item.getContent().isEmpty()) {
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(item.getContent());
            } else {
                binding.tvMessage.setVisibility(View.GONE);
            }

            loadAttachments(binding.llAttachments, item.getAttachments());
        }
    }
}