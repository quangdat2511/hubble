package com.example.hubble.view.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

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

    public static void start(Context context, ScopeType scopeType, @Nullable String scopeId) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(EXTRA_SCOPE_TYPE, scopeType.name());
        intent.putExtra(EXTRA_SCOPE_ID, scopeId);
        context.startActivity(intent);
    }

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;

    private ScopeType scopeType;
    @Nullable private String scopeId;

    private final List<Category> categories = new ArrayList<>();
    private final List<String> tabTitles = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String scopeTypeName = getIntent().getStringExtra(EXTRA_SCOPE_TYPE);
        scopeType = scopeTypeName != null ? ScopeType.valueOf(scopeTypeName) : ScopeType.CHANNEL;
        scopeId = getIntent().getStringExtra(EXTRA_SCOPE_ID);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.init(this, scopeType, scopeId);

        buildTabs();
        setupViewPager();
        setupToolbar();
        setupSearchInput();
    }

    private void buildTabs() {
        categories.clear();
        tabTitles.clear();
        switch (scopeType) {
            case CHANNEL:
                categories.add(Category.MESSAGES);
                tabTitles.add(getString(R.string.search_tab_messages));
                categories.add(Category.MEMBERS);
                tabTitles.add(getString(R.string.search_tab_members));
                categories.add(Category.MEDIA);
                tabTitles.add(getString(R.string.search_tab_media));
                categories.add(Category.FILES);
                tabTitles.add(getString(R.string.search_tab_files));
                categories.add(Category.PINS);
                tabTitles.add(getString(R.string.search_tab_pins));
                break;
            case SERVER:
                categories.add(Category.MESSAGES);
                tabTitles.add(getString(R.string.search_tab_messages));
                categories.add(Category.MEMBERS);
                tabTitles.add(getString(R.string.search_tab_members));
                categories.add(Category.CHANNELS);
                tabTitles.add(getString(R.string.search_tab_channels));
                categories.add(Category.MEDIA);
                tabTitles.add(getString(R.string.search_tab_media));
                categories.add(Category.FILES);
                tabTitles.add(getString(R.string.search_tab_files));
                categories.add(Category.PINS);
                tabTitles.add(getString(R.string.search_tab_pins));
                break;
            case DM:
                categories.add(Category.FRIENDS);
                tabTitles.add(getString(R.string.search_tab_friends));
                categories.add(Category.MEDIA);
                tabTitles.add(getString(R.string.search_tab_media));
                categories.add(Category.FILES);
                tabTitles.add(getString(R.string.search_tab_files));
                break;
        }
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new SearchPagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles.get(position))
        ).attach();
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
                    categories.get(position), scopeType, scopeId);
        }
    }
}
