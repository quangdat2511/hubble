package com.example.hubble.view.emoji;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.emoji.EmojiPanelAdapter;
import com.example.hubble.adapter.gif.GifAdapter;
import com.example.hubble.data.EmojiData;
import com.example.hubble.data.api.GiphyClient;
import com.example.hubble.data.model.emoji.EmojiCategory;
import com.example.hubble.data.model.gif.GiphyGif;
import com.example.hubble.data.model.gif.GiphyResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmojiPickerView extends FrameLayout {

    // ── Listener interfaces ────────────────────────────────────────────────

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    public interface OnMediaSelectedListener {
        void onGifSelected(String gifUrl, String previewUrl, String title);
        void onStickerSelected(String stickerUrl, String previewUrl, String title);
    }

    // ── Constants ──────────────────────────────────────────────────────────

    private static final int EMOJI_SPAN_COUNT = 8;
    private static final int GIF_SPAN_COUNT = 2;
    private static final int STICKER_SPAN_COUNT = 3;
    private static final int DEBOUNCE_MS = 500;

    private static final int TAB_EMOJI = 0;
    private static final int TAB_GIF = 1;
    private static final int TAB_STICKER = 2;

    // ── Views ──────────────────────────────────────────────────────────────

    private View tabEmoji, tabGif, tabSticker;
    private View tabIndicatorEmoji, tabIndicatorGif, tabIndicatorSticker;

    // Emoji tab
    private EditText etEmojiSearch;
    private RecyclerView rvEmojis;
    private View categoryBar;
    private LinearLayout categoryIconsContainer;
    private View emojiContent;

    // GIF tab
    private View gifContent;
    private EditText etGifSearch;
    private RecyclerView rvGifs;
    private TextView tvGifLabel;

    // Sticker tab
    private View stickerContent;
    private EditText etStickerSearch;
    private RecyclerView rvStickers;
    private TextView tvStickerLabel;

    // ── Adapters ───────────────────────────────────────────────────────────

    private EmojiPanelAdapter emojiAdapter;
    private GifAdapter gifAdapter;
    private GifAdapter stickerAdapter;

    // ── State ──────────────────────────────────────────────────────────────

    private int currentTab = TAB_EMOJI;
    private boolean gifTrendingLoaded = false;
    private boolean stickerTrendingLoaded = false;

    // ── Debounce helpers ───────────────────────────────────────────────────

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable gifDebounce;
    private Runnable stickerDebounce;

    // ── Listeners ──────────────────────────────────────────────────────────

    private OnEmojiSelectedListener emojiSelectedListener;
    private OnMediaSelectedListener mediaSelectedListener;

    // ── Constructors ───────────────────────────────────────────────────────

    public EmojiPickerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public EmojiPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmojiPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ── Init ───────────────────────────────────────────────────────────────

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_emoji_picker, this, true);
        bindViews();
        setupEmojiTab();
        setupGifTab();
        setupStickerTab();
        setupTabs();
        switchTab(TAB_EMOJI);
    }

    private void bindViews() {
        tabEmoji = findViewById(R.id.tabEmoji);
        tabGif = findViewById(R.id.tabGif);
        tabSticker = findViewById(R.id.tabSticker);
        tabIndicatorEmoji = findViewById(R.id.tabIndicatorEmoji);
        tabIndicatorGif = findViewById(R.id.tabIndicatorGif);
        tabIndicatorSticker = findViewById(R.id.tabIndicatorSticker);

        emojiContent = findViewById(R.id.emojiContent);
        etEmojiSearch = findViewById(R.id.etEmojiSearch);
        rvEmojis = findViewById(R.id.rvEmojis);
        categoryBar = findViewById(R.id.categoryBar);
        categoryIconsContainer = findViewById(R.id.categoryIconsContainer);

        gifContent = findViewById(R.id.gifContent);
        etGifSearch = findViewById(R.id.etGifSearch);
        rvGifs = findViewById(R.id.rvGifs);
        tvGifLabel = findViewById(R.id.tvGifLabel);

        stickerContent = findViewById(R.id.stickerContent);
        etStickerSearch = findViewById(R.id.etStickerSearch);
        rvStickers = findViewById(R.id.rvStickers);
        tvStickerLabel = findViewById(R.id.tvStickerLabel);
    }

    // ── Emoji tab ──────────────────────────────────────────────────────────

    private void setupEmojiTab() {
        emojiAdapter = new EmojiPanelAdapter();
        emojiAdapter.setOnEmojiClickListener(emoji -> {
            if (emojiSelectedListener != null) emojiSelectedListener.onEmojiSelected(emoji);
        });

        GridLayoutManager glm = new GridLayoutManager(getContext(), EMOJI_SPAN_COUNT);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return emojiAdapter.getItemViewType(position) == EmojiPanelAdapter.TYPE_HEADER
                        ? EMOJI_SPAN_COUNT : 1;
            }
        });

        rvEmojis.setLayoutManager(glm);
        rvEmojis.setAdapter(emojiAdapter);
        emojiAdapter.setCategories(EmojiData.CATEGORIES);

        setupEmojiSearch();
        setupCategoryBar();
    }

    private void setupEmojiSearch() {
        etEmojiSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    emojiAdapter.setCategories(EmojiData.CATEGORIES);
                    categoryBar.setVisibility(VISIBLE);
                } else {
                    categoryBar.setVisibility(GONE);
                    List<String> results = EmojiData.search(query);
                    emojiAdapter.setSearchResults(results, getContext().getString(R.string.emoji_results));
                }
            }
        });
    }

    private void setupCategoryBar() {
        categoryIconsContainer.removeAllViews();
        for (EmojiCategory cat : EmojiData.CATEGORIES) {
            TextView iconView = new TextView(getContext());
            iconView.setText(cat.icon);
            iconView.setTextSize(20f);
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            iconView.setPadding(pad, pad, pad, pad);
            iconView.setOnClickListener(v -> scrollToCategory(cat));
            categoryIconsContainer.addView(iconView);
        }
    }

    private void scrollToCategory(EmojiCategory targetCat) {
        int pos = 0;
        for (EmojiCategory cat : EmojiData.CATEGORIES) {
            if (cat == targetCat) break;
            pos += 1 + cat.emojis.size();
        }
        rvEmojis.scrollToPosition(pos);
    }

    // ── GIF tab ────────────────────────────────────────────────────────────

    private void setupGifTab() {
        gifAdapter = new GifAdapter();
        gifAdapter.setOnGifClickListener(gif -> {
            if (mediaSelectedListener != null && gif.getOriginalUrl() != null) {
                mediaSelectedListener.onGifSelected(gif.getOriginalUrl(), gif.getPreviewUrl(), gif.title);
            }
        });

        StaggeredGridLayoutManager gifLm =
                new StaggeredGridLayoutManager(GIF_SPAN_COUNT, StaggeredGridLayoutManager.VERTICAL);
        rvGifs.setLayoutManager(gifLm);
        rvGifs.setAdapter(gifAdapter);

        etGifSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                debounceHandler.removeCallbacks(gifDebounce);
                if (query.isEmpty()) {
                    tvGifLabel.setText(R.string.emoji_trending);
                    loadTrendingGifs(true);
                } else {
                    tvGifLabel.setText(R.string.emoji_results);
                    gifDebounce = () -> searchGifs(query);
                    debounceHandler.postDelayed(gifDebounce, DEBOUNCE_MS);
                }
            }
        });
    }

    private void loadTrendingGifs(boolean forceRefresh) {
        if (gifTrendingLoaded && !forceRefresh) return;
        GiphyClient.get().getTrendingGifs(
                GiphyClient.API_KEY,
                GiphyClient.defaultLimit(),
                GiphyClient.rating()
        ).enqueue(new Callback<GiphyResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyResponse> call,
                                   @NonNull Response<GiphyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    gifAdapter.setData(response.body().data);
                    gifTrendingLoaded = true;
                }
            }

            @Override
            public void onFailure(@NonNull Call<GiphyResponse> call, @NonNull Throwable t) {}
        });
    }

    private void searchGifs(String query) {
        GiphyClient.get().searchGifs(
                GiphyClient.API_KEY, query,
                GiphyClient.defaultLimit(), 0,
                GiphyClient.rating()
        ).enqueue(new Callback<GiphyResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyResponse> call,
                                   @NonNull Response<GiphyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    gifAdapter.setData(response.body().data);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GiphyResponse> call, @NonNull Throwable t) {}
        });
    }

    // ── Sticker tab ────────────────────────────────────────────────────────

    private void setupStickerTab() {
        stickerAdapter = new GifAdapter();
        stickerAdapter.setOnGifClickListener(gif -> {
            if (mediaSelectedListener != null && gif.getOriginalUrl() != null) {
                mediaSelectedListener.onStickerSelected(gif.getOriginalUrl(), gif.getPreviewUrl(), gif.title);
            }
        });

        GridLayoutManager stickerLm = new GridLayoutManager(getContext(), STICKER_SPAN_COUNT);
        rvStickers.setLayoutManager(stickerLm);
        rvStickers.setAdapter(stickerAdapter);

        etStickerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                debounceHandler.removeCallbacks(stickerDebounce);
                if (query.isEmpty()) {
                    tvStickerLabel.setText(R.string.emoji_trending);
                    loadTrendingStickers(true);
                } else {
                    tvStickerLabel.setText(R.string.emoji_results);
                    stickerDebounce = () -> searchStickers(query);
                    debounceHandler.postDelayed(stickerDebounce, DEBOUNCE_MS);
                }
            }
        });
    }

    private void loadTrendingStickers(boolean forceRefresh) {
        if (stickerTrendingLoaded && !forceRefresh) return;
        GiphyClient.get().getTrendingStickers(
                GiphyClient.API_KEY,
                GiphyClient.defaultLimit(),
                GiphyClient.rating()
        ).enqueue(new Callback<GiphyResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyResponse> call,
                                   @NonNull Response<GiphyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stickerAdapter.setData(response.body().data);
                    stickerTrendingLoaded = true;
                }
            }

            @Override
            public void onFailure(@NonNull Call<GiphyResponse> call, @NonNull Throwable t) {}
        });
    }

    private void searchStickers(String query) {
        GiphyClient.get().searchStickers(
                GiphyClient.API_KEY, query,
                GiphyClient.defaultLimit(), 0,
                GiphyClient.rating()
        ).enqueue(new Callback<GiphyResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyResponse> call,
                                   @NonNull Response<GiphyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stickerAdapter.setData(response.body().data);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GiphyResponse> call, @NonNull Throwable t) {}
        });
    }

    // ── Tab switching ──────────────────────────────────────────────────────

    private void setupTabs() {
        tabEmoji.setOnClickListener(v -> switchTab(TAB_EMOJI));
        tabGif.setOnClickListener(v -> switchTab(TAB_GIF));
        tabSticker.setOnClickListener(v -> switchTab(TAB_STICKER));
    }

    private void switchTab(int tab) {
        currentTab = tab;

        tabIndicatorEmoji.setVisibility(tab == TAB_EMOJI ? VISIBLE : INVISIBLE);
        tabIndicatorGif.setVisibility(tab == TAB_GIF ? VISIBLE : INVISIBLE);
        tabIndicatorSticker.setVisibility(tab == TAB_STICKER ? VISIBLE : INVISIBLE);

        emojiContent.setVisibility(tab == TAB_EMOJI ? VISIBLE : GONE);
        gifContent.setVisibility(tab == TAB_GIF ? VISIBLE : GONE);
        stickerContent.setVisibility(tab == TAB_STICKER ? VISIBLE : GONE);

        // Lazy-load trending on first open
        if (tab == TAB_GIF) loadTrendingGifs(false);
        if (tab == TAB_STICKER) loadTrendingStickers(false);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.emojiSelectedListener = listener;
    }

    public void setOnMediaSelectedListener(OnMediaSelectedListener listener) {
        this.mediaSelectedListener = listener;
    }
}
