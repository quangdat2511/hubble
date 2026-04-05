package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.databinding.ActivityChangeCategoryBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangeCategoryActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CURRENT_PARENT_ID = "current_parent_id";

    private static final ConcurrentHashMap<String, List<CategoryItem>> categoryCache = new ConcurrentHashMap<>();

    private ActivityChangeCategoryBinding binding;
    private String serverId;
    private String currentParentId;

    public static Intent createIntent(Context context, String serverId, String currentParentId) {
        Intent intent = new Intent(context, ChangeCategoryActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CURRENT_PARENT_ID, currentParentId);
        return intent;
    }

    public static void prefetch(Context context, String serverId) {
        if (serverId == null || categoryCache.containsKey(serverId)) return;
        TokenManager tm = new TokenManager(context);
        String token = "Bearer " + tm.getAccessToken();
        RetrofitClient.getServerService(context).getServerChannels(token, serverId)
                .enqueue(new Callback<ApiResponse<List<ChannelDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelDto>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            List<CategoryItem> items = new ArrayList<>();
                            items.add(new CategoryItem(null, "Uncategorized"));
                            for (ChannelDto ch : response.body().getResult()) {
                                if ("CATEGORY".equalsIgnoreCase(ch.getType())) {
                                    items.add(new CategoryItem(ch.getId(), ch.getName()));
                                }
                            }
                            categoryCache.put(serverId, items);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelDto>>> c,
                                         @NonNull Throwable t) {}
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityChangeCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        currentParentId = getIntent().getStringExtra(EXTRA_CURRENT_PARENT_ID);

        if (serverId == null) { finish(); return; }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));

        loadCategories();
    }

    private void applyEdgeToEdge() {
        final int origTop = binding.getRoot().getPaddingTop();
        final int origBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), origTop + bars.top,
                    v.getPaddingRight(), origBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void loadCategories() {
        // Cache-first: show cached categories immediately
        List<CategoryItem> cached = categoryCache.get(serverId);
        if (cached != null) {
            binding.rvCategories.setAdapter(new CategoryAdapter(cached));
        }

        // Always fetch fresh in background
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this).getServerChannels(token, serverId)
                .enqueue(new Callback<ApiResponse<List<ChannelDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelDto>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            List<CategoryItem> items = new ArrayList<>();
                            items.add(new CategoryItem(null,
                                    getString(R.string.channel_settings_uncategorized)));
                            for (ChannelDto ch : response.body().getResult()) {
                                if ("CATEGORY".equalsIgnoreCase(ch.getType())) {
                                    items.add(new CategoryItem(ch.getId(), ch.getName()));
                                }
                            }
                            categoryCache.put(serverId, items);
                            binding.rvCategories.setAdapter(new CategoryAdapter(items));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelDto>>> call,
                                         @NonNull Throwable t) {
                        if (cached == null) {
                            Snackbar.make(binding.getRoot(),
                                    getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void onCategorySelected(CategoryItem item) {
        Intent data = new Intent();
        data.putExtra("selected_category_id", item.id);
        data.putExtra("selected_category_name", item.id != null ? item.name : null);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    // ── Inner classes ──────────────────────────────────────────────────────

    private static class CategoryItem {
        final String id;
        final String name;
        CategoryItem(String id, String name) { this.id = id; this.name = name; }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        private final List<CategoryItem> items;
        CategoryAdapter(List<CategoryItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_pick, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryItem item = items.get(position);
            holder.tvName.setText(item.name);
            boolean selected = (currentParentId == null && item.id == null)
                    || (currentParentId != null && currentParentId.equals(item.id));
            holder.ivCheck.setVisibility(selected ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> onCategorySelected(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvName;
            final ImageView ivCheck;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                ivCheck = itemView.findViewById(R.id.ivCheck);
            }
        }
    }
}
