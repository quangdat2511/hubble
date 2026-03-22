package com.example.hubble.view.emoji;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.adapter.emoji.EmojiPanelAdapter;
import com.example.hubble.data.EmojiData;
import com.example.hubble.data.model.emoji.EmojiCategory;

import java.util.List;

public class EmojiPickerView extends FrameLayout {

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    private static final int SPAN_COUNT = 8;
    private static final int TAB_EMOJI = 0;
    private static final int TAB_GIF = 1;
    private static final int TAB_STICKER = 2;

    private View tabEmoji, tabGif, tabSticker;
    private View tabIndicatorEmoji, tabIndicatorGif, tabIndicatorSticker;
    private EditText etSearch;
    private RecyclerView rvEmojis;
    private View categoryBar;
    private LinearLayout categoryIconsContainer;
    private View emojiContent, gifContent, stickerContent;

    private EmojiPanelAdapter emojiAdapter;
    private OnEmojiSelectedListener emojiSelectedListener;
    private int currentTab = TAB_EMOJI;

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

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_emoji_picker, this, true);

        tabEmoji = findViewById(R.id.tabEmoji);
        tabGif = findViewById(R.id.tabGif);
        tabSticker = findViewById(R.id.tabSticker);
        tabIndicatorEmoji = findViewById(R.id.tabIndicatorEmoji);
        tabIndicatorGif = findViewById(R.id.tabIndicatorGif);
        tabIndicatorSticker = findViewById(R.id.tabIndicatorSticker);

        etSearch = findViewById(R.id.etEmojiSearch);
        rvEmojis = findViewById(R.id.rvEmojis);
        categoryBar = findViewById(R.id.categoryBar);
        categoryIconsContainer = findViewById(R.id.categoryIconsContainer);
        emojiContent = findViewById(R.id.emojiContent);
        gifContent = findViewById(R.id.gifContent);
        stickerContent = findViewById(R.id.stickerContent);

        setupEmojiRecyclerView();
        setupTabs();
        setupSearch();
        setupCategoryBar();
        switchTab(TAB_EMOJI);
    }

    private void setupEmojiRecyclerView() {
        emojiAdapter = new EmojiPanelAdapter();
        emojiAdapter.setOnEmojiClickListener(emoji -> {
            if (emojiSelectedListener != null) emojiSelectedListener.onEmojiSelected(emoji);
        });

        GridLayoutManager glm = new GridLayoutManager(getContext(), SPAN_COUNT);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return emojiAdapter.getItemViewType(position) == EmojiPanelAdapter.TYPE_HEADER
                        ? SPAN_COUNT : 1;
            }
        });

        rvEmojis.setLayoutManager(glm);
        rvEmojis.setAdapter(emojiAdapter);
        emojiAdapter.setCategories(EmojiData.CATEGORIES);
    }

    private void setupTabs() {
        tabEmoji.setOnClickListener(v -> switchTab(TAB_EMOJI));
        tabGif.setOnClickListener(v -> switchTab(TAB_GIF));
        tabSticker.setOnClickListener(v -> switchTab(TAB_STICKER));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
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
                    emojiAdapter.setSearchResults(results);
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

    private void switchTab(int tab) {
        currentTab = tab;

        tabIndicatorEmoji.setVisibility(tab == TAB_EMOJI ? VISIBLE : INVISIBLE);
        tabIndicatorGif.setVisibility(tab == TAB_GIF ? VISIBLE : INVISIBLE);
        tabIndicatorSticker.setVisibility(tab == TAB_STICKER ? VISIBLE : INVISIBLE);

        emojiContent.setVisibility(tab == TAB_EMOJI ? VISIBLE : GONE);
        gifContent.setVisibility(tab == TAB_GIF ? VISIBLE : GONE);
        stickerContent.setVisibility(tab == TAB_STICKER ? VISIBLE : GONE);
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.emojiSelectedListener = listener;
    }
}
