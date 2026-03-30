package com.example.hubble.adapter.gif;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.hubble.R;
import com.example.hubble.data.model.gif.GiphyGif;

import java.util.ArrayList;
import java.util.List;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.GifViewHolder> {

    public interface OnGifClickListener {
        void onGifClick(GiphyGif gif);
    }

    private final List<GiphyGif> items = new ArrayList<>();
    private OnGifClickListener listener;

    public void setOnGifClickListener(OnGifClickListener l) {
        this.listener = l;
    }

    public void setData(List<GiphyGif> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendData(List<GiphyGif> more) {
        if (more == null || more.isEmpty()) return;
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public GifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_picker, parent, false);
        return new GifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GifViewHolder holder, int position) {
        GiphyGif gif = items.get(position);
        holder.bind(gif, listener);
    }

    @Override
    public void onViewRecycled(@NonNull GifViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.ivMedia.getContext()).clear(holder.ivMedia);
    }

    static class GifViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivMedia;
        final ProgressBar progressBar;

        GifViewHolder(View view) {
            super(view);
            ivMedia = view.findViewById(R.id.ivMedia);
            progressBar = view.findViewById(R.id.progressBar);
        }

        void bind(GiphyGif gif, OnGifClickListener listener) {
            String url = gif.getPreviewUrl();
            progressBar.setVisibility(View.VISIBLE);

            Glide.with(ivMedia.getContext())
                    .asGif()
                    .load(url)
                    .listener(new RequestListener<GifDrawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model,
                                Target<GifDrawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, Object model,
                                Target<GifDrawable> target, DataSource dataSource,
                                boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivMedia);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onGifClick(gif);
            });
        }
    }
}
