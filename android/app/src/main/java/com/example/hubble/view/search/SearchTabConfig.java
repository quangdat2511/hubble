package com.example.hubble.view.search;

import com.example.hubble.R;
import com.example.hubble.view.search.SearchResultFragment.Category;
import com.example.hubble.viewmodel.SearchViewModel.ScopeType;

import java.util.ArrayList;
import java.util.List;

public final class SearchTabConfig {
    private SearchTabConfig() {}

    public static List<Category> getBaseCategories(ScopeType scopeType) {
        List<Category> categories = new ArrayList<>();
        switch (scopeType) {
            case CHANNEL:
            case SERVER:
                categories.add(Category.MEMBERS);
                categories.add(Category.CHANNELS);
                break;
            case DM:
                categories.add(Category.FRIENDS);
                break;
        }
        return categories;
    }

    public static int getTabTitleRes(Category category) {
        switch (category) {
            case MESSAGES:
                return R.string.search_tab_messages;
            case MEMBERS:
                return R.string.search_tab_members;
            case CHANNELS:
                return R.string.search_tab_channels;
            case FRIENDS:
                return R.string.search_tab_friends;
            default:
                return R.string.search_tab_messages;
        }
    }
}
