package com.example.hubble.view.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivitySearchBinding;
import com.example.hubble.view.search.SearchResultFragment.Category;
import com.example.hubble.viewmodel.SearchViewModel;
import com.example.hubble.viewmodel.SearchViewModel.ScopeType;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String EXTRA_SCOPE_TYPE = "search_scope_type";
    private static final String EXTRA_SCOPE_ID = "search_scope_id";
    private static final String EXTRA_SERVER_NAME = "search_server_name";

    public static void start(Context context, ScopeType scopeType, @Nullable String scopeId) {
        start(context, scopeType, scopeId, null);
    }

    public static void start(Context context, ScopeType scopeType, @Nullable String scopeId,
                             @Nullable String serverName) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(EXTRA_SCOPE_TYPE, scopeType.name());
        intent.putExtra(EXTRA_SCOPE_ID, scopeId);
        intent.putExtra(EXTRA_SERVER_NAME, serverName);
        context.startActivity(intent);
    }

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;

    private ScopeType scopeType;
    @Nullable private String scopeId;
    @Nullable private String serverName;

    private final List<Category> categories = new ArrayList<>();
    private final List<String> tabTitles = new ArrayList<>();
    private TabLayoutMediator tabLayoutMediator;
    private boolean messagesTabVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        String scopeTypeName = getIntent().getStringExtra(EXTRA_SCOPE_TYPE);
        scopeType = scopeTypeName != null ? ScopeType.valueOf(scopeTypeName) : ScopeType.CHANNEL;
        scopeId = getIntent().getStringExtra(EXTRA_SCOPE_ID);
        serverName = getIntent().getStringExtra(EXTRA_SERVER_NAME);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.init(this, scopeType, scopeId);

        buildTabs(false);
        setupViewPager();
        setupToolbar();
        setupSearchInput();

        viewModel.currentQuery.observe(this, query -> {
            boolean hasQuery = query != null && !query.trim().isEmpty();
            rebuildAdapter(hasQuery);
        });
    }

    private void buildTabs(boolean includeMessages) {
        categories.clear();
        tabTitles.clear();
        if (includeMessages) {
            categories.add(Category.MESSAGES);
            tabTitles.add(getString(R.string.search_tab_messages));
        }
        for (Category category : SearchTabConfig.getBaseCategories(scopeType)) {
            categories.add(category);
            tabTitles.add(getString(SearchTabConfig.getTabTitleRes(category)));
        }
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new SearchPagerAdapter(this));
        tabLayoutMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles.get(position)));
        tabLayoutMediator.attach();
    }

    private void rebuildAdapter(boolean showMessages) {
        if (messagesTabVisible == showMessages) return;
        messagesTabVisible = showMessages;
        buildTabs(showMessages);
        if (tabLayoutMediator != null) tabLayoutMediator.detach();
        binding.viewPager.setAdapter(new SearchPagerAdapter(this));
        tabLayoutMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles.get(position)));
        tabLayoutMediator.attach();
        if (showMessages) binding.viewPager.setCurrentItem(0, false);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.etSearch.requestFocus();
    }

    private void setupSearchInput() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.onQueryChanged(s != null ? s.toString() : "");
            }
        });
    }

    private class SearchPagerAdapter extends FragmentStateAdapter {
        SearchPagerAdapter(FragmentActivity fa) { super(fa); }

        @Override
        public int getItemCount() { return categories.size(); }

        @Override
        public Fragment createFragment(int position) {
            return SearchResultFragment.newInstance(
                    categories.get(position), scopeType, scopeId, serverName);
        }
    }
}
